package cv.perola.modules.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.dto.AppointmentActionNotesRequest;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CancelAppointmentRequest;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.dto.RejectRescheduleRequest;
import cv.perola.modules.booking.dto.RescheduleAppointmentRequest;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.dto.RegisterRequest;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
class BookingIntegrationTest {

    private static final String TENANT_HEADER = "X-Business-Id";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    private String ownerToken;
    private UUID ownerId;
    private String customerToken;
    private UUID customerId;
    private UUID businessId;
    private UUID employeeId;
    private UUID serviceId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String ownerEmail = "owner-booking-" + suffix + "@perola.cv";
        String customerEmail = "cust-booking-" + suffix + "@perola.cv";

        AuthResponse ownerReg = registerUser("Dona Salão", ownerEmail, "+23891" + suffix.substring(0, 5));
        ownerId = ownerReg.user().id();
        businessId = createBusiness(ownerReg.accessToken(), "Salão Booking " + suffix);
        ownerToken = loginUser(ownerEmail, "SenhaForte123!").accessToken();
        employeeId = getFirstEmployeeId(ownerToken, businessId);
        serviceId = createService(ownerToken, businessId, "Tranças Box Braids", 60, new BigDecimal("3500.00"));

        AuthResponse customerReg = registerUser("Cliente Maria", customerEmail, "+23892" + suffix.substring(0, 5));
        customerToken = customerReg.accessToken();
        customerId = customerReg.user().id();
    }

    @AfterEach
    void tearDown() {
        if (businessId != null) {
            jdbcTemplate.update("DELETE FROM appointments WHERE business_id = ?", businessId);
            jdbcTemplate.update("DELETE FROM employee_services WHERE business_id = ?", businessId);
            jdbcTemplate.update("DELETE FROM employees WHERE business_id = ?", businessId);
            jdbcTemplate.update("DELETE FROM services WHERE business_id = ?", businessId);
            jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", businessId);
        }
        if (customerId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", customerId);
        }
        if (ownerId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", ownerId);
        }
    }

    @Test
    @DisplayName("Deve criar agendamento com status PENDING, snapshots imutáveis e cálculo de end_time")
    void shouldCreateAppointmentWithPendingStatusAndImmutableServiceSnapshots() throws Exception {
        // Arrange
        OffsetDateTime startTime = OffsetDateTime.of(2027, 11, 10, 9, 0, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                startTime,
                "Por favor preparar extensões pretas."
        );

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.businessId").value(businessId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.serviceId").value(serviceId.toString()))
                .andExpect(jsonPath("$.serviceName").value("Tranças Box Braids"))
                .andExpect(jsonPath("$.servicePrice").value(3500.00))
                .andExpect(jsonPath("$.serviceDuration").value(60))
                .andExpect(jsonPath("$.startTime").value("2027-11-10T09:00:00-01:00"))
                .andExpect(jsonPath("$.endTime").value("2027-11-10T10:00:00-01:00"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value("Por favor preparar extensões pretas."))
                .andReturn();

        AppointmentResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), AppointmentResponse.class
        );

        // Alterar preço e nome do serviço no catálogo e verificar que o snapshot permanece inalterado
        UpdateServiceRequest updateServiceRequest = new UpdateServiceRequest(
                "Tranças Premium", "Nova descrição", 90, new BigDecimal("5000.00"), true
        );
        mockMvc.perform(patch("/api/v1/businesses/{id}/services/{serviceId}", businessId, serviceId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateServiceRequest)))
                .andExpect(status().isOk());

        AppointmentEntity persisted = appointmentRepository.findById(created.id()).orElseThrow();
        assertThat(persisted.getServiceName()).isEqualTo("Tranças Box Braids");
        assertThat(persisted.getServicePrice()).isEqualByComparingTo("3500.00");
        assertThat(persisted.getServiceDuration()).isEqualTo(60);
    }

    @Test
    @DisplayName("Deve bloquear overbooking via constraint GiST ex_appointments_no_overlap e responder HTTP 409 RFC 7807")
    void shouldRejectOverlappingAppointmentWithHttp409ProblemDetails() throws Exception {
        // Arrange
        OffsetDateTime firstStart = OffsetDateTime.of(2027, 11, 10, 14, 0, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest firstRequest = new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                firstStart,
                "Agendamento 1 (14:00 - 15:00)"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Tentativa de agendamento sobreposto às 14:30 (colide com [14:00, 15:00))
        OffsetDateTime overlappingStart = OffsetDateTime.of(2027, 11, 10, 14, 30, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest overlappingRequest = new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                overlappingStart,
                "Agendamento sobreposto (14:30 - 15:30)"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/slot-already-booked"))
                .andExpect(jsonPath("$.title").value("Conflito de Horário"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "O profissional selecionado já possui um agendamento confirmado ou pendente neste intervalo de tempo."
                ));

        // Agendamento contíguo imediatamente às 15:00 ([15:00, 16:00)) deve ser permitido pelo intervalo semi-aberto [)
        OffsetDateTime adjacentStart = OffsetDateTime.of(2027, 11, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest adjacentRequest = new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                adjacentStart,
                "Agendamento contíguo (15:00 - 16:00)"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjacentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("2027-11-10T15:00:00-01:00"))
                .andExpect(jsonPath("$.endTime").value("2027-11-10T16:00:00-01:00"));
    }

    @Test
    @DisplayName("Deve rejeitar criação de agendamento sem token JWT com HTTP 401 Unauthorized")
    void shouldRejectUnauthenticatedAppointmentCreationWithHttp401() throws Exception {
        // Arrange
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                OffsetDateTime.of(2027, 11, 10, 10, 0, 0, 0, ZoneOffset.ofHours(-1)),
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve executar ciclo de vida completo com reagendamento, histórico append-only e listagens")
    void shouldExecuteFullAppointmentLifecycleAndRecordAppendOnlyAuditHistory() throws Exception {
        // Arrange: 1. Criar agendamento (PENDING)
        OffsetDateTime initialStart = OffsetDateTime.of(2027, 11, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest createReq = new CreateAppointmentRequest(
                businessId, employeeId, serviceId, initialStart, "Reserva inicial"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        UUID appointmentId = objectMapper.readValue(
                createRes.getResponse().getContentAsString(), AppointmentResponse.class
        ).id();

        // Act & Assert: 2. Confirmar agendamento pelo OWNER (PENDING -> CONFIRMED)
        mockMvc.perform(post("/api/v1/appointments/{id}/confirm", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentActionNotesRequest("Confirmado pelo salão"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 3. Propor reagendamento pelo OWNER (CONFIRMED -> RESCHEDULE_PENDING)
        OffsetDateTime firstProposal = OffsetDateTime.of(2027, 11, 15, 11, 30, 0, 0, ZoneOffset.ofHours(-1));
        mockMvc.perform(patch("/api/v1/appointments/{id}/reschedule", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RescheduleAppointmentRequest(firstProposal, "Ajuste de agenda"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULE_PENDING"))
                .andExpect(jsonPath("$.startTime").value("2027-11-15T10:00:00-01:00"))
                .andExpect(jsonPath("$.proposedStartTime").value("2027-11-15T11:30:00-01:00"))
                .andExpect(jsonPath("$.proposedEndTime").value("2027-11-15T12:30:00-01:00"));

        // 4. Recusar proposta pelo cliente (RESCHEDULE_PENDING -> CONFIRMED, mantendo horário original)
        mockMvc.perform(post("/api/v1/appointments/{id}/reschedule/reject", appointmentId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RejectRescheduleRequest("Não consigo às 11:30"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startTime").value("2027-11-15T10:00:00-01:00"))
                .andExpect(jsonPath("$.proposedStartTime").isEmpty())
                .andExpect(jsonPath("$.proposedEndTime").isEmpty());

        // 5. Propor segundo reagendamento pelo cliente (CONFIRMED -> RESCHEDULE_PENDING)
        OffsetDateTime secondProposal = OffsetDateTime.of(2027, 11, 15, 16, 0, 0, 0, ZoneOffset.ofHours(-1));
        mockMvc.perform(patch("/api/v1/appointments/{id}/reschedule", appointmentId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RescheduleAppointmentRequest(secondProposal, "Posso às 16:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULE_PENDING"))
                .andExpect(jsonPath("$.proposedStartTime").value("2027-11-15T16:00:00-01:00"));

        // 6. Aceitar segundo reagendamento pelo OWNER (RESCHEDULE_PENDING -> CONFIRMED com novo horário)
        mockMvc.perform(post("/api/v1/appointments/{id}/reschedule/accept", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AppointmentActionNotesRequest("Novo horário confirmado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startTime").value("2027-11-15T16:00:00-01:00"))
                .andExpect(jsonPath("$.endTime").value("2027-11-15T17:00:00-01:00"))
                .andExpect(jsonPath("$.proposedStartTime").isEmpty())
                .andExpect(jsonPath("$.proposedEndTime").isEmpty());

        // 7. Concluir agendamento pelo OWNER (CONFIRMED -> COMPLETED)
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AppointmentActionNotesRequest("Atendimento excelente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 8. Consultar detalhes e linha do tempo de auditoria (7 eventos na ordem cronológica)
        mockMvc.perform(get("/api/v1/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.history.length()").value(7))
                .andExpect(jsonPath("$.history[0].action").value("CREATED"))
                .andExpect(jsonPath("$.history[1].action").value("CONFIRMED"))
                .andExpect(jsonPath("$.history[2].action").value("RESCHEDULE_REQUESTED"))
                .andExpect(jsonPath("$.history[3].action").value("RESCHEDULE_REJECTED"))
                .andExpect(jsonPath("$.history[4].action").value("RESCHEDULE_REQUESTED"))
                .andExpect(jsonPath("$.history[5].action").value("RESCHEDULE_CONFIRMED"))
                .andExpect(jsonPath("$.history[6].action").value("COMPLETED"));

        // 9. Consultar listagens GET /my e GET /appointments (com X-Business-Id)
        mockMvc.perform(get("/api/v1/appointments/my")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(appointmentId.toString()));

        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .param("employeeId", employeeId.toString())
                        .param("startDate", "2027-11-01")
                        .param("endDate", "2027-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(appointmentId.toString()));
    }

    @Test
    @DisplayName("Deve rejeitar transições de estado inválidas com HTTP 400 e libertar slot na constraint GiST após cancelamento")
    void shouldRejectInvalidStateTransitionWithHttp400AndFreeSlotAfterCancellation() throws Exception {
        // Arrange: criar agendamento às 11:00
        OffsetDateTime slotStart = OffsetDateTime.of(2027, 11, 20, 11, 0, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest createReq = new CreateAppointmentRequest(
                businessId, employeeId, serviceId, slotStart, "Reserva para cancelar"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID appointmentId = objectMapper.readValue(
                createRes.getResponse().getContentAsString(), AppointmentResponse.class
        ).id();

        // Cancelar o agendamento (PENDING -> CANCELLED)
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelAppointmentRequest("Não poderei comparecer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Tentar transitar de CANCELLED para COMPLETED -> deve falhar com HTTP 400 Bad Request
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TENANT_HEADER, businessId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentActionNotesRequest("Tentativa inválida"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Como o agendamento anterior está CANCELLED, a constraint GiST parcial ignora o registo e permite nova reserva no mesmo horário
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
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
                name, "Santiago", "Praia", "Achada Santo António", "+2382621122", "Atlantic/Cape_Verde"
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
        return UUID.fromString(
                objectMapper.readTree(res.getResponse().getContentAsString()).get(0).get("id").asText()
        );
    }

    private UUID createService(String token, UUID bizId, String name, int duration, BigDecimal price)
            throws Exception {
        CreateServiceRequest req = new CreateServiceRequest(name, "Serviço profissional", duration, price);
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
