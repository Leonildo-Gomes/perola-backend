package cv.perola.modules.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
class ScheduleProfilesAndMonthlyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private String ownerToken;
    private String businessId;
    private String ownerEmployeeId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        RegisterRequest registerReq = new RegisterRequest(
                "Dono Salão Schedule " + uniqueSuffix,
                "owner.sched." + uniqueSuffix + "@perola.cv",
                "+23899" + uniqueSuffix.replaceAll("[^0-9]", "1").substring(0, 5),
                "SenhaForte123!"
        );

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String initialToken = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        CreateBusinessRequest createBizReq = new CreateBusinessRequest(
                "Salão Schedule " + uniqueSuffix,
                "Santiago",
                "Praia",
                "Achada Santo António",
                "+2382610000",
                "Atlantic/Cape_Verde"
        );

        MvcResult bizResult = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBizReq)))
                .andExpect(status().isCreated())
                .andReturn();

        businessId = objectMapper.readTree(bizResult.getResponse().getContentAsString())
                .get("id").asText();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + registerReq.email() + "\",\"password\":\"SenhaForte123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        ownerToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        MvcResult empListResult = mockMvc.perform(get("/api/v1/businesses/" + businessId + "/employees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId))
                .andExpect(status().isOk())
                .andReturn();

        ownerEmployeeId = objectMapper.readTree(empListResult.getResponse().getContentAsString())
                .get(0).get("id").asText();
    }

    @Test
    @DisplayName("Deve permitir criar Horário Padrão (08:30-16:00) para Novembro e novo Horário de Dezembro (09:30-18:00) sem conflito entre meses")
    void shouldCreateMultipleScheduleProfilesAndOpenDifferentMonthsIndependently() throws Exception {
        // 1. Criar Horário Padrão (Segunda das 08:30 às 16:00 com pausa das 12:30 às 13:00; Domingo folga)
        SaveEmployeeScheduleRequest novemberScheduleReq = new SaveEmployeeScheduleRequest(
                "Horário Padrão",
                true,
                true,
                buildSevenDays(LocalTime.of(8, 30), LocalTime.of(16, 0))
        );

        MvcResult createNovSchedRes = mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novemberScheduleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Horário Padrão"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].startTime").value("08:30:00"))
                .andExpect(jsonPath("$.days[0].endTime").value("16:00:00"))
                .andExpect(jsonPath("$.days[0].breaks[0].startTime").value("12:30:00"))
                .andExpect(jsonPath("$.days[0].breaks[0].endTime").value("13:00:00"))
                .andExpect(jsonPath("$.days[6].dayOfWeek").value("SUNDAY"))
                .andExpect(jsonPath("$.days[6].isWorking").value(false))
                .andReturn();

        UUID novemberScheduleId = UUID.fromString(
                objectMapper.readTree(createNovSchedRes.getResponse().getContentAsString()).get("id").asText()
        );

        // 2. Abrir Novembro de 2026 com o Horário Padrão e folga no feriado 01/11/2026
        OpenMonthlyScheduleRequest openNovReq = new OpenMonthlyScheduleRequest(
                novemberScheduleId,
                2026,
                11,
                List.of(new HolidayDecisionRequest(LocalDate.of(2026, 11, 1), false))
        );

        mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/monthly/open")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openNovReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(novemberScheduleId.toString()))
                .andExpect(jsonPath("$.scheduleName").value("Horário Padrão"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(11))
                .andExpect(jsonPath("$.isOpened").value(true));

        // Verificar que a folga de feriado (01/11/2026) foi criada automaticamente em time-off
        mockMvc.perform(get("/api/v1/schedule/employees/" + ownerEmployeeId + "/time-off")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startDate").value("2026-11-01"))
                .andExpect(jsonPath("$[0].type").value("HOLIDAY"))
                .andExpect(jsonPath("$[0].isAllDay").value(true));

        // 3. Criar um segundo perfil de horário para Dezembro (Segunda das 09:30 às 18:00 com pausa das 12:30 às 13:00)
        SaveEmployeeScheduleRequest decemberScheduleReq = new SaveEmployeeScheduleRequest(
                "Horário de Dezembro",
                false,
                true,
                buildSevenDays(LocalTime.of(9, 30), LocalTime.of(18, 0))
        );

        MvcResult createDecSchedRes = mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decemberScheduleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Horário de Dezembro"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.days[0].startTime").value("09:30:00"))
                .andExpect(jsonPath("$.days[0].endTime").value("18:00:00"))
                .andReturn();

        UUID decemberScheduleId = UUID.fromString(
                objectMapper.readTree(createDecSchedRes.getResponse().getContentAsString()).get("id").asText()
        );

        // 4. Abrir Dezembro de 2026 selecionando o Horário de Dezembro
        OpenMonthlyScheduleRequest openDecReq = new OpenMonthlyScheduleRequest(
                decemberScheduleId,
                2026,
                12,
                List.of(new HolidayDecisionRequest(LocalDate.of(2026, 12, 25), false))
        );

        mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/monthly/open")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openDecReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(decemberScheduleId.toString()))
                .andExpect(jsonPath("$.scheduleName").value("Horário de Dezembro"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(12))
                .andExpect(jsonPath("$.isOpened").value(true));

        // 5. Confirmar que Novembro continua associado ao Horário Padrão e Dezembro ao Horário de Dezembro!
        mockMvc.perform(get("/api/v1/schedule/employees/" + ownerEmployeeId + "/monthly")
                        .param("year", "2026")
                        .param("month", "11")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(novemberScheduleId.toString()))
                .andExpect(jsonPath("$.scheduleName").value("Horário Padrão"));

        mockMvc.perform(get("/api/v1/schedule/employees/" + ownerEmployeeId + "/monthly")
                        .param("year", "2026")
                        .param("month", "12")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(decemberScheduleId.toString()))
                .andExpect(jsonPath("$.scheduleName").value("Horário de Dezembro"));
    }

    @Test
    @DisplayName("Deve permitir atualizar um perfil de horário existente via PUT e listar perfis via GET")
    void shouldUpdateAndListScheduleProfilesSuccessfully() throws Exception {
        SaveEmployeeScheduleRequest createReq = new SaveEmployeeScheduleRequest(
                "Horário Inicial",
                true,
                true,
                buildSevenDays(LocalTime.of(8, 0), LocalTime.of(16, 0))
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String scheduleId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        SaveEmployeeScheduleRequest updateReq = new SaveEmployeeScheduleRequest(
                "Horário Atualizado",
                true,
                true,
                buildSevenDays(LocalTime.of(9, 0), LocalTime.of(17, 0))
        );

        mockMvc.perform(put("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Horário Atualizado"))
                .andExpect(jsonPath("$.days[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.days[0].endTime").value("17:00:00"));

        mockMvc.perform(get("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Horário Atualizado"));
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 400 Bad Request quando pausa estiver fora do expediente ou menos de 7 dias")
    void shouldRejectInvalidScheduleConfigurationWith400() throws Exception {
        SaveEmployeeScheduleRequest invalidReq = new SaveEmployeeScheduleRequest(
                "Horário Inválido",
                true,
                true,
                List.of(new ScheduleDayItemRequest(
                        DayOfWeek.MONDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0), Collections.emptyList()
                ))
        );

        mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 Forbidden quando funcionário tenta alterar horário ou abrir mês de outro colega")
    void shouldRejectEmployeeModifyingAnotherColleagueScheduleWith403() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest empUserReg = new RegisterRequest(
                "Colaborador Dois",
                "emp2." + uniqueSuffix + "@perola.cv",
                "+23898" + uniqueSuffix.replaceAll("[^0-9]", "2").substring(0, 5),
                "SenhaForte123!"
        );

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empUserReg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode empUserJson = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("user");
        UUID empUserId = UUID.fromString(empUserJson.get("id").asText());

        CreateEmployeeRequest addEmpReq = new CreateEmployeeRequest(empUserId, "Colaborador Dois");
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/employees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addEmpReq)))
                .andExpect(status().isCreated());

        MvcResult empLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + empUserReg.email() + "\",\"password\":\"SenhaForte123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String emp2Token = objectMapper.readTree(empLoginRes.getResponse().getContentAsString())
                .get("accessToken").asText();

        SaveEmployeeScheduleRequest scheduleReq = new SaveEmployeeScheduleRequest(
                "Tentativa Indevida",
                true,
                true,
                buildSevenDays(LocalTime.of(8, 30), LocalTime.of(16, 0))
        );

        // Colaborador 2 tenta criar horário para o dono (ownerEmployeeId) -> 403 Forbidden
        mockMvc.perform(post("/api/v1/schedule/employees/" + ownerEmployeeId + "/schedules")
                        .header("Authorization", "Bearer " + emp2Token)
                        .header("X-Business-Id", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private List<ScheduleDayItemRequest> buildSevenDays(LocalTime mondayStart, LocalTime mondayEnd) {
        return List.of(
                new ScheduleDayItemRequest(
                        DayOfWeek.MONDAY,
                        true,
                        mondayStart,
                        mondayEnd,
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
                ),
                new ScheduleDayItemRequest(
                        DayOfWeek.TUESDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0),
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
                ),
                new ScheduleDayItemRequest(
                        DayOfWeek.WEDNESDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0),
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
                ),
                new ScheduleDayItemRequest(
                        DayOfWeek.THURSDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0),
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
                ),
                new ScheduleDayItemRequest(
                        DayOfWeek.FRIDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0),
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
                ),
                new ScheduleDayItemRequest(
                        DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0),
                        Collections.emptyList()
                ),
                new ScheduleDayItemRequest(
                        DayOfWeek.SUNDAY, false, null, null, Collections.emptyList()
                )
        );
    }
}

