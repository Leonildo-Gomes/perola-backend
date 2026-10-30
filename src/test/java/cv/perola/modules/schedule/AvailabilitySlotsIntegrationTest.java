package cv.perola.modules.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.HolidayDecisionRequest;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AvailabilitySlotsIntegrationTest {

    private static final String TENANT_HEADER = "X-Business-Id";
    private static final ZoneId CAPE_VERDE_ZONE = ZoneId.of("Atlantic/Cape_Verde");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    private UUID ownerUserId;
    private String ownerToken;
    private UUID businessId;
    private UUID employeeId;
    private UUID serviceId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String ownerEmail = "owner-slots-" + suffix + "@perola.cv";

        AuthResponse regAuth = registerUser("Sofia Slots", ownerEmail, "+23895" + suffix.substring(0, 5));
        ownerUserId = regAuth.user().id();
        businessId = createBusiness(regAuth.accessToken(), "Salão Sofia " + suffix);
        ownerToken = loginUser(ownerEmail, "SenhaForte123!").accessToken();
        employeeId = getFirstEmployeeId(ownerToken, businessId);
        serviceId = createService(ownerToken, businessId, "Corte & Brushing", 60, new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("Deve retornar 0 slots quando o mês consultado ainda não foi aberto")
    void shouldReturnZeroSlotsWhenMonthIsNotOpened() throws Exception {
        // Arrange
        SaveEmployeeScheduleRequest novRequest = buildSevenDayRequest(
                "Horário Padrão",
                true,
                LocalTime.of(8, 30),
                LocalTime.of(16, 0),
                List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
        );
        mockMvc.perform(post("/api/v1/schedule/employees/{id}/schedules", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novRequest)))
                .andExpect(status().isCreated());

        // Act & Assert (Public endpoint without JWT or X-Business-Id)
        mockMvc.perform(get("/api/v1/availability/slots")
                        .param("businessId", businessId.toString())
                        .param("employeeId", employeeId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2027-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2027-11-02"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.slots", hasSize(0)));
    }

    @Test
    @DisplayName("Deve calcular slots diferentes para Novembro (08:30–16:00) e Dezembro (09:30–18:00) conforme o perfil associado ao mês")
    void shouldCalculateDistinctSlotsForNovemberAndDecemberUsingTheirRespectiveScheduleProfiles() throws Exception {
        // Arrange: 1. Criar Horário de Novembro (Seg 08:30–16:00, pausa 12:30–13:00 -> 7 slots de 60m)
        SaveEmployeeScheduleRequest novProfileReq = buildSevenDayRequest(
                "Horário Novembro",
                true,
                LocalTime.of(8, 30),
                LocalTime.of(16, 0),
                List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
        );
        MvcResult novRes = mockMvc.perform(post("/api/v1/schedule/employees/{id}/schedules", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novProfileReq)))
                .andExpect(status().isCreated())
                .andReturn();
        EmployeeScheduleResponse novSchedule = objectMapper.readValue(
                novRes.getResponse().getContentAsString(), EmployeeScheduleResponse.class);

        // 2. Criar Horário de Dezembro (Seg 09:30–18:00, pausa 12:30–13:00 -> 3 manhã + 5 tarde = 8 slots de 60m)
        SaveEmployeeScheduleRequest decProfileReq = buildSevenDayRequest(
                "Horário Dezembro",
                false,
                LocalTime.of(9, 30),
                LocalTime.of(18, 0),
                List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
        );
        MvcResult decRes = mockMvc.perform(post("/api/v1/schedule/employees/{id}/schedules", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decProfileReq)))
                .andExpect(status().isCreated())
                .andReturn();
        EmployeeScheduleResponse decSchedule = objectMapper.readValue(
                decRes.getResponse().getContentAsString(), EmployeeScheduleResponse.class);

        // 3. Abrir Novembro 2027 com Horário Novembro (e decidir folgar em 2027-11-01)
        OpenMonthlyScheduleRequest openNov = new OpenMonthlyScheduleRequest(
                novSchedule.id(),
                2027,
                11,
                List.of(new HolidayDecisionRequest(LocalDate.of(2027, 11, 1), false))
        );
        mockMvc.perform(post("/api/v1/schedule/employees/{id}/monthly/open", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openNov)))
                .andExpect(status().isOk());

        // 4. Abrir Dezembro 2027 com Horário Dezembro
        OpenMonthlyScheduleRequest openDec = new OpenMonthlyScheduleRequest(
                decSchedule.id(),
                2027,
                12,
                List.of()
        );
        mockMvc.perform(post("/api/v1/schedule/employees/{id}/monthly/open", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openDec)))
                .andExpect(status().isOk());

        // Act & Assert A: Feriado em 2027-11-01 (Segunda-feira, mas com workOnHoliday = false) -> 0 slots
        mockMvc.perform(get("/api/v1/availability/slots")
                        .param("employeeId", employeeId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2027-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(0)));

        // Act & Assert B: Segunda-feira 2027-11-08 (Novembro: 08:30–16:00 com pausa 12:30–13:00 -> 7 slots)
        mockMvc.perform(get("/api/v1/availability/slots")
                        .param("employeeId", employeeId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2027-11-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(7)))
                .andExpect(jsonPath("$.slots[0].startTime").value("2027-11-08T08:30:00-01:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("2027-11-08T09:30:00-01:00"))
                .andExpect(jsonPath("$.slots[3].startTime").value("2027-11-08T11:30:00-01:00"))
                .andExpect(jsonPath("$.slots[3].endTime").value("2027-11-08T12:30:00-01:00"))
                .andExpect(jsonPath("$.slots[4].startTime").value("2027-11-08T13:00:00-01:00"))
                .andExpect(jsonPath("$.slots[4].endTime").value("2027-11-08T14:00:00-01:00"))
                .andExpect(jsonPath("$.slots[6].startTime").value("2027-11-08T15:00:00-01:00"))
                .andExpect(jsonPath("$.slots[6].endTime").value("2027-11-08T16:00:00-01:00"));

        // Act & Assert C: Segunda-feira 2027-12-06 (Dezembro: 09:30–18:00 com pausa 12:30–13:00 -> 8 slots)
        mockMvc.perform(get("/api/v1/availability/slots")
                        .param("employeeId", employeeId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2027-12-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(8)))
                .andExpect(jsonPath("$.slots[0].startTime").value("2027-12-06T09:30:00-01:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("2027-12-06T10:30:00-01:00"))
                .andExpect(jsonPath("$.slots[3].startTime").value("2027-12-06T13:00:00-01:00"))
                .andExpect(jsonPath("$.slots[7].endTime").value("2027-12-06T18:00:00-01:00"));

        // Act & Assert D: Domingo 2027-11-07 (isWorking = false) -> 0 slots
        mockMvc.perform(get("/api/v1/availability/slots")
                        .param("employeeId", employeeId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2027-11-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(0)));
    }

    @Test
    @DisplayName("Deve subtrair ausências parciais e agendamentos ativos (PENDING/CONFIRMED), ignorando agendamentos CANCELLED")
    void shouldSubtractPartialTimeOffAndActiveAppointmentsWhileIgnoringCancelledAppointments() throws Exception {
        // Arrange: Horário 08:00–16:00 com pausa 12:00–13:00 (7 slots originais: 08-09, 09-10, 10-11, 11-12, 13-14, 14-15, 15-16)
        SaveEmployeeScheduleRequest scheduleReq = buildSevenDayRequest(
                "Horário Padrão",
                true,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0)))
        );
        MvcResult createRes = mockMvc.perform(post("/api/v1/schedule/employees/{id}/schedules", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isCreated())
                .andReturn();
        EmployeeScheduleResponse schedule = objectMapper.readValue(
                createRes.getResponse().getContentAsString(), EmployeeScheduleResponse.class);

        OpenMonthlyScheduleRequest openReq = new OpenMonthlyScheduleRequest(schedule.id(), 2027, 11, List.of());
        mockMvc.perform(post("/api/v1/schedule/employees/{id}/monthly/open", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isOk());

        LocalDate targetMonday = LocalDate.of(2027, 11, 15);

        // Criar ausência parcial 14:00–15:00
        CreateTimeOffRequest partialOff = new CreateTimeOffRequest(
                targetMonday,
                targetMonday,
                false,
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                TimeOffType.PERSONAL,
                "Consulta médica"
        );
        mockMvc.perform(post("/api/v1/schedule/employees/{id}/time-off", employeeId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialOff)))
                .andExpect(status().isCreated());

        // Inserir agendamento CONFIRMED 09:00–10:00 e agendamento CANCELLED 10:00–11:00
        insertAppointment(targetMonday, LocalTime.of(9, 0), LocalTime.of(10, 0), "CONFIRMED");
        insertAppointment(targetMonday, LocalTime.of(10, 0), LocalTime.of(11, 0), "CANCELLED");

        // Act & Assert: Slots restantes devem ser 5 (08-09, 10-11, 11-12, 13-14, 15-16)
        mockMvc.perform(get("/api/v1/availability/slots")
                        .param("employeeId", employeeId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2027-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(5)))
                .andExpect(jsonPath("$.slots[0].startTime").value("2027-11-15T08:00:00-01:00"))
                .andExpect(jsonPath("$.slots[1].startTime").value("2027-11-15T10:00:00-01:00"))
                .andExpect(jsonPath("$.slots[2].startTime").value("2027-11-15T11:00:00-01:00"))
                .andExpect(jsonPath("$.slots[3].startTime").value("2027-11-15T13:00:00-01:00"))
                .andExpect(jsonPath("$.slots[4].startTime").value("2027-11-15T15:00:00-01:00"));
    }

    private void insertAppointment(LocalDate date, LocalTime start, LocalTime end, String status) {
        Timestamp startTs = Timestamp.from(ZonedDateTime.of(date, start, CAPE_VERDE_ZONE).toInstant());
        Timestamp endTs = Timestamp.from(ZonedDateTime.of(date, end, CAPE_VERDE_ZONE).toInstant());
        jdbcTemplate.update("""
                INSERT INTO appointments (
                    business_id, customer_id, employee_id, service_id, created_by_id,
                    start_time, end_time, service_name, service_price, service_duration, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'Corte & Brushing', 2500.00, 60, CAST(? AS appointment_status))
                """,
                businessId, ownerUserId, employeeId, serviceId, ownerUserId,
                startTs, endTs, status
        );
    }

    private SaveEmployeeScheduleRequest buildSevenDayRequest(
            String name,
            Boolean isDefault,
            LocalTime mondayStart,
            LocalTime mondayEnd,
            List<BreakItemRequest> mondayBreaks
    ) {
        List<ScheduleDayItemRequest> days = new ArrayList<>();
        days.add(new ScheduleDayItemRequest(DayOfWeek.MONDAY, true, mondayStart, mondayEnd, mondayBreaks));
        days.add(new ScheduleDayItemRequest(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0), List.of()));
        days.add(new ScheduleDayItemRequest(DayOfWeek.WEDNESDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0), List.of()));
        days.add(new ScheduleDayItemRequest(DayOfWeek.THURSDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0), List.of()));
        days.add(new ScheduleDayItemRequest(DayOfWeek.FRIDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0), List.of()));
        days.add(new ScheduleDayItemRequest(DayOfWeek.SATURDAY, false, null, null, List.of()));
        days.add(new ScheduleDayItemRequest(DayOfWeek.SUNDAY, false, null, null, List.of()));
        return new SaveEmployeeScheduleRequest(name, isDefault, true, days);
    }

    private AuthResponse registerUser(String name, String email, String phone) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, phone, "SenhaForte123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }

    private AuthResponse loginUser(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }

    private UUID createBusiness(String token, String name) throws Exception {
        CreateBusinessRequest req = new CreateBusinessRequest(
                name, "Santiago", "Praia", "Platô", "+2382610011", "Atlantic/Cape_Verde"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), BusinessResponse.class).id();
    }

    private UUID getFirstEmployeeId(String token, UUID bizId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/businesses/{id}/employees", bizId)
                        .header("Authorization", "Bearer " + token)
                        .header(TENANT_HEADER, bizId.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).get(0).get("id").asText());
    }

    private UUID createService(String token, UUID bizId, String name, int durationMinutes, BigDecimal price) throws Exception {
        CreateServiceRequest req = new CreateServiceRequest(name, "Serviço completo", durationMinutes, price);
        MvcResult res = mockMvc.perform(post("/api/v1/businesses/{id}/services", bizId)
                        .header("Authorization", "Bearer " + token)
                        .header(TENANT_HEADER, bizId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), ServiceResponse.class).id();
    }
}
