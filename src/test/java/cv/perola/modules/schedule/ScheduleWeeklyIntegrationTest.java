package cv.perola.modules.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.security.JwtService;
import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.TenantMembership;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.SetWeeklyScheduleRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemRequest;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ScheduleWeeklyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeScheduleRepository scheduleRepository;

    @Autowired
    private EmployeeScheduleBreakRepository breakRepository;

    private UserEntity ownerUser;
    private UserEntity employeeUser;
    private BusinessEntity business;
    private EmployeeEntity employee;
    private String ownerToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        ownerUser = userRepository.save(UserEntity.builder()
                .name("Owner User")
                .email("owner." + UUID.randomUUID() + "@perola.cv")
                .phone("+23899" + (int) (Math.random() * 90000 + 10000))
                .passwordHash("pwd")
                .isActive(true)
                .build());

        employeeUser = userRepository.save(UserEntity.builder()
                .name("Employee User")
                .email("emp." + UUID.randomUUID() + "@perola.cv")
                .phone("+23898" + (int) (Math.random() * 90000 + 10000))
                .passwordHash("pwd")
                .isActive(true)
                .build());

        business = businessRepository.save(BusinessEntity.builder()
                .ownerId(ownerUser.getId())
                .name("Salão Beleza " + UUID.randomUUID())
                .island("Santiago")
                .city("Praia")
                .location("Plateau")
                .contactPhone("+2382610000")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build());

        employee = employeeRepository.save(EmployeeEntity.builder()
                .businessId(business.getId())
                .userId(employeeUser.getId())
                .displayName("Carlos Cabeleireiro")
                .isActive(true)
                .build());

        ownerToken = jwtService.generateAccessToken(
                ownerUser.getId(),
                ownerUser.getEmail(),
                List.of(UserRole.OWNER.name()),
                List.of(new TenantMembership(business.getId(), UserRole.OWNER.name()))
        );

        employeeToken = jwtService.generateAccessToken(
                employeeUser.getId(),
                employeeUser.getEmail(),
                List.of(UserRole.EMPLOYEE.name()),
                List.of(new TenantMembership(business.getId(), UserRole.EMPLOYEE.name()))
        );
    }

    @Test
    @DisplayName("Deve configurar e consultar expediente semanal com pausas para OWNER com sucesso")
    void shouldSetAndGetWeeklyScheduleSuccessfullyForOwner() throws Exception {
        // Arrange
        BreakItemRequest breakItem = new BreakItemRequest("Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0));
        WeeklyScheduleItemRequest scheduleItem = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                true,
                List.of(breakItem)
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(scheduleItem));

        // Act & Assert (PUT)
        mockMvc.perform(put("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.schedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.schedules[0].startTime").value("08:00:00"))
                .andExpect(jsonPath("$.schedules[0].endTime").value("18:00:00"))
                .andExpect(jsonPath("$.schedules[0].breaks[0].title").value("Almoço"))
                .andExpect(jsonPath("$.schedules[0].breaks[0].startTime").value("12:00:00"))
                .andExpect(jsonPath("$.schedules[0].breaks[0].endTime").value("13:00:00"));

        // Act & Assert (GET)
        mockMvc.perform(get("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.schedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.schedules[0].breaks[0].title").value("Almoço"));

        assertThat(scheduleRepository.findAllByEmployeeId(employee.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Deve permitir ao próprio colaborador configurar e consultar o seu expediente")
    void shouldSetAndGetWeeklyScheduleSuccessfullyForSelfEmployee() throws Exception {
        // Arrange
        WeeklyScheduleItemRequest scheduleItem = new WeeklyScheduleItemRequest(
                DayOfWeek.TUESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                true,
                List.of()
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(scheduleItem));

        // Act & Assert (PUT)
        mockMvc.perform(put("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules[0].dayOfWeek").value("TUESDAY"));

        // Act & Assert (GET)
        mockMvc.perform(get("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules[0].dayOfWeek").value("TUESDAY"));
    }

    @Test
    @DisplayName("Deve rejeitar com 400 Bad Request quando pausa estiver fora do turno de trabalho")
    void shouldRejectScheduleWhenBreakOutsideWorkingHours() throws Exception {
        // Arrange
        BreakItemRequest breakItem = new BreakItemRequest("Pausa Tarde Demais", LocalTime.of(17, 30), LocalTime.of(18, 30));
        WeeklyScheduleItemRequest scheduleItem = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                true,
                List.of(breakItem)
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(scheduleItem));

        // Act & Assert
        mockMvc.perform(put("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("deve estar totalmente contida no turno")));
    }

    @Test
    @DisplayName("Deve rejeitar com 400 Bad Request quando houver sobreposição entre pausas")
    void shouldRejectScheduleWhenBreaksOverlap() throws Exception {
        // Arrange
        BreakItemRequest b1 = new BreakItemRequest("Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0));
        BreakItemRequest b2 = new BreakItemRequest("Descanso", LocalTime.of(12, 30), LocalTime.of(13, 30));
        WeeklyScheduleItemRequest scheduleItem = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                true,
                List.of(b1, b2)
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(scheduleItem));

        // Act & Assert
        mockMvc.perform(put("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("sobrepõem-se no dia MONDAY")));
    }

    @Test
    @DisplayName("Deve rejeitar com 400 Bad Request quando endTime não for posterior a startTime")
    void shouldRejectScheduleWhenEndTimeBeforeStartTime() throws Exception {
        // Arrange
        WeeklyScheduleItemRequest scheduleItem = new WeeklyScheduleItemRequest(
                DayOfWeek.WEDNESDAY,
                LocalTime.of(18, 0),
                LocalTime.of(8, 0),
                true,
                List.of()
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(scheduleItem));

        // Act & Assert
        mockMvc.perform(put("/api/v1/schedule/employees/{employeeId}/weekly", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"));
    }

    @Test
    @DisplayName("Deve rejeitar com 403 Forbidden quando EMPLOYEE tenta gerir expediente de outro colaborador")
    void shouldRejectScheduleWhenAccessingAnotherEmployeeWithoutOwnerRole() throws Exception {
        // Arrange
        EmployeeEntity otherEmployee = employeeRepository.save(EmployeeEntity.builder()
                .businessId(business.getId())
                .userId(UUID.randomUUID())
                .displayName("Outro Funcionário")
                .isActive(true)
                .build());

        WeeklyScheduleItemRequest scheduleItem = new WeeklyScheduleItemRequest(
                DayOfWeek.FRIDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                true,
                List.of()
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(scheduleItem));

        // Act & Assert
        mockMvc.perform(put("/api/v1/schedule/employees/{employeeId}/weekly", otherEmployee.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/tenant-access-denied"));
    }
}
