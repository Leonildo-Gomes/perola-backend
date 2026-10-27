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
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ScheduleTimeOffIntegrationTest {

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
    private EmployeeTimeOffRepository timeOffRepository;

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
                .name("Owner TimeOff")
                .email("owner.to." + UUID.randomUUID() + "@perola.cv")
                .phone("+23899" + (int) (Math.random() * 90000 + 10000))
                .passwordHash("pwd")
                .isActive(true)
                .build());

        employeeUser = userRepository.save(UserEntity.builder()
                .name("Employee TimeOff")
                .email("emp.to." + UUID.randomUUID() + "@perola.cv")
                .phone("+23898" + (int) (Math.random() * 90000 + 10000))
                .passwordHash("pwd")
                .isActive(true)
                .build());

        business = businessRepository.save(BusinessEntity.builder()
                .ownerId(ownerUser.getId())
                .name("Salão Férias " + UUID.randomUUID())
                .island("São Vicente")
                .city("Mindelo")
                .location("Rua Lisboa")
                .contactPhone("+2382320000")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build());

        employee = employeeRepository.save(EmployeeEntity.builder()
                .businessId(business.getId())
                .userId(employeeUser.getId())
                .displayName("Joana Esteticista")
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
    @DisplayName("Deve consultar feriados de Cabo Verde publicamente sem token JWT")
    void shouldListHolidaysPubliclyWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/holidays?year=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("Deve criar, listar e remover ausência para o profissional com sucesso")
    void shouldCreateListAndDeleteTimeOffSuccessfully() throws Exception {
        // 1. Criar ausência (POST)
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 15),
                true,
                null,
                null,
                TimeOffType.VACATION,
                "Férias anuais"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedule/employees/{employeeId}/time-off", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.startDate").value("2026-11-10"))
                .andExpect(jsonPath("$.endDate").value("2026-11-15"))
                .andExpect(jsonPath("$.type").value("VACATION"))
                .andReturn();

        String timeOffId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Listar ausências (GET)
        mockMvc.perform(get("/api/v1/schedule/employees/{employeeId}/time-off", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(timeOffId));

        // 3. Remover ausência (DELETE)
        mockMvc.perform(delete("/api/v1/schedule/employees/{employeeId}/time-off/{timeOffId}", employee.getId(), timeOffId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString()))
                .andExpect(status().isNoContent());

        assertThat(timeOffRepository.findByIdAndEmployeeId(UUID.fromString(timeOffId), employee.getId())).isEmpty();
    }

    @Test
    @DisplayName("Deve permitir ao próprio colaborador registar e consultar a sua ausência")
    void shouldAllowSelfEmployeeToCreateAndListTimeOff() throws Exception {
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 1),
                false,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                TimeOffType.PERSONAL,
                "Assuntos pessoais"
        );

        mockMvc.perform(post("/api/v1/schedule/employees/{employeeId}/time-off", employee.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isAllDay").value(false))
                .andExpect(jsonPath("$.startTime").value("14:00:00"))
                .andExpect(jsonPath("$.endTime").value("18:00:00"));

        mockMvc.perform(get("/api/v1/schedule/employees/{employeeId}/time-off", employee.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Deve rejeitar criação com 400 Bad Request quando endDate for anterior a startDate")
    void shouldRejectTimeOffWhenEndDateBeforeStartDate() throws Exception {
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 10),
                true,
                null,
                null,
                TimeOffType.VACATION,
                null
        );

        mockMvc.perform(post("/api/v1/schedule/employees/{employeeId}/time-off", employee.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("não pode ser anterior à data de início")));
    }

    @Test
    @DisplayName("Deve rejeitar criação com 403 Forbidden quando EMPLOYEE tenta criar ausência para outro")
    void shouldRejectWhenEmployeeTriesToCreateForAnother() throws Exception {
        EmployeeEntity other = employeeRepository.save(EmployeeEntity.builder()
                .businessId(business.getId())
                .userId(UUID.randomUUID())
                .displayName("Outro")
                .isActive(true)
                .build());

        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 15),
                true,
                null,
                null,
                TimeOffType.VACATION,
                null
        );

        mockMvc.perform(post("/api/v1/schedule/employees/{employeeId}/time-off", other.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .header(TenantFilter.TENANT_HEADER, business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/tenant-access-denied"));
    }
}
