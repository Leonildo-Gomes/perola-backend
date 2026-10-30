package cv.perola.modules.booking.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentActionNotesRequest;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CancelAppointmentRequest;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.dto.RejectRescheduleRequest;
import cv.perola.modules.booking.dto.RescheduleAppointmentRequest;
import cv.perola.modules.booking.usecase.AcceptRescheduleUseCase;
import cv.perola.modules.booking.usecase.CancelAppointmentUseCase;
import cv.perola.modules.booking.usecase.CompleteAppointmentUseCase;
import cv.perola.modules.booking.usecase.ConfirmAppointmentUseCase;
import cv.perola.modules.booking.usecase.CreateAppointmentUseCase;
import cv.perola.modules.booking.usecase.GetAppointmentUseCase;
import cv.perola.modules.booking.usecase.ListMyAppointmentsUseCase;
import cv.perola.modules.booking.usecase.ListSalonAppointmentsUseCase;
import cv.perola.modules.booking.usecase.ProposeRescheduleUseCase;
import cv.perola.modules.booking.usecase.RejectRescheduleUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private CreateAppointmentUseCase createAppointmentUseCase;

    @Mock
    private GetAppointmentUseCase getAppointmentUseCase;

    @Mock
    private ConfirmAppointmentUseCase confirmAppointmentUseCase;

    @Mock
    private CompleteAppointmentUseCase completeAppointmentUseCase;

    @Mock
    private CancelAppointmentUseCase cancelAppointmentUseCase;

    @Mock
    private ProposeRescheduleUseCase proposeRescheduleUseCase;

    @Mock
    private AcceptRescheduleUseCase acceptRescheduleUseCase;

    @Mock
    private RejectRescheduleUseCase rejectRescheduleUseCase;

    @Mock
    private ListMyAppointmentsUseCase listMyAppointmentsUseCase;

    @Mock
    private ListSalonAppointmentsUseCase listSalonAppointmentsUseCase;

    @InjectMocks
    private AppointmentController appointmentController;

    @Test
    @DisplayName("Deve retornar HTTP 201 Created ao criar agendamento com utilizador autenticado")
    void shouldReturnCreatedWhenAppointmentIsCreated() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                startTime,
                "Notas"
        );
        AppointmentResponse expected = createSampleResponse(userId, startTime, AppointmentStatus.PENDING);
        when(createAppointmentUseCase.execute(userId, request)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.createAppointment(principal, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(createAppointmentUseCase).execute(userId, request);
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException quando o principal for nulo")
    void shouldThrowAuthenticationFailedExceptionWhenPrincipalIsNull() {
        // Arrange
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.ofHours(-1)),
                null
        );

        // Act & Assert
        assertThatThrownBy(() -> appointmentController.createAppointment(null, request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Autenticação necessária.");
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK com detalhes e histórico ao consultar agendamento por ID")
    void shouldReturnOkWhenGettingAppointmentById() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        AppointmentResponse expected = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.CONFIRMED
        );
        when(getAppointmentUseCase.execute(appointmentId, principal)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.getAppointmentById(appointmentId, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(getAppointmentUseCase).execute(appointmentId, principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao confirmar agendamento")
    void shouldReturnOkWhenConfirmingAppointment() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        AppointmentActionNotesRequest request = new AppointmentActionNotesRequest("Confirmado na receção");
        AppointmentResponse expected = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.CONFIRMED
        );
        when(confirmAppointmentUseCase.execute(appointmentId, "Confirmado na receção", principal)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.confirmAppointment(
                appointmentId, request, principal
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(confirmAppointmentUseCase).execute(appointmentId, "Confirmado na receção", principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao concluir agendamento")
    void shouldReturnOkWhenCompletingAppointment() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        AppointmentActionNotesRequest request = new AppointmentActionNotesRequest("Serviço concluído");
        AppointmentResponse expected = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.COMPLETED
        );
        when(completeAppointmentUseCase.execute(appointmentId, "Serviço concluído", principal)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.completeAppointment(
                appointmentId, request, principal
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(completeAppointmentUseCase).execute(appointmentId, "Serviço concluído", principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao cancelar agendamento")
    void shouldReturnOkWhenCancellingAppointment() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        CancelAppointmentRequest request = new CancelAppointmentRequest("Imprevisto pessoal");
        AppointmentResponse expected = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.CANCELLED
        );
        when(cancelAppointmentUseCase.execute(appointmentId, "Imprevisto pessoal", principal)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.cancelAppointment(
                appointmentId, request, principal
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(cancelAppointmentUseCase).execute(appointmentId, "Imprevisto pessoal", principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao propor reagendamento")
    void shouldReturnOkWhenProposingReschedule() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        OffsetDateTime proposedStart = OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.ofHours(-1));
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(proposedStart, "Mudança de turno");
        AppointmentResponse expected = createSampleResponse(
                userId, proposedStart, AppointmentStatus.RESCHEDULE_PENDING
        );
        when(proposeRescheduleUseCase.execute(appointmentId, request, principal)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.proposeReschedule(
                appointmentId, request, principal
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(proposeRescheduleUseCase).execute(appointmentId, request, principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao aceitar proposta de reagendamento")
    void shouldReturnOkWhenAcceptingReschedule() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        AppointmentActionNotesRequest request = new AppointmentActionNotesRequest("Horário aceite");
        AppointmentResponse expected = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.CONFIRMED
        );
        when(acceptRescheduleUseCase.execute(appointmentId, "Horário aceite", principal)).thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.acceptReschedule(
                appointmentId, request, principal
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(acceptRescheduleUseCase).execute(appointmentId, "Horário aceite", principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao recusar proposta de reagendamento")
    void shouldReturnOkWhenRejectingReschedule() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        RejectRescheduleRequest request = new RejectRescheduleRequest("Prefiro manter o horário original");
        AppointmentResponse expected = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.CONFIRMED
        );
        when(rejectRescheduleUseCase.execute(appointmentId, "Prefiro manter o horário original", principal))
                .thenReturn(expected);

        // Act
        ResponseEntity<AppointmentResponse> response = appointmentController.rejectReschedule(
                appointmentId, request, principal
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(rejectRescheduleUseCase).execute(appointmentId, "Prefiro manter o horário original", principal);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK com página de agendamentos do cliente autenticado")
    void shouldReturnOkWhenListingCustomerAppointments() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        AppointmentResponse item = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.CONFIRMED
        );
        PageResponse<AppointmentResponse> expected = new PageResponse<>(
                List.of(item), 0, 20, 1L, 1
        );
        when(listMyAppointmentsUseCase.execute(userId, AppointmentStatus.CONFIRMED, 0, 20)).thenReturn(expected);

        // Act
        ResponseEntity<PageResponse<AppointmentResponse>> response = appointmentController.getMyAppointments(
                principal, AppointmentStatus.CONFIRMED, 0, 20
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(listMyAppointmentsUseCase).execute(userId, AppointmentStatus.CONFIRMED, 0, 20);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK com página de agendamentos da agenda do salão")
    void shouldReturnOkWhenListingSalonAppointments() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        UserPrincipal principal = createPrincipal(userId);
        AppointmentResponse item = createSampleResponse(
                userId, OffsetDateTime.now(ZoneOffset.ofHours(-1)), AppointmentStatus.PENDING
        );
        PageResponse<AppointmentResponse> expected = new PageResponse<>(
                List.of(item), 0, 20, 1L, 1
        );
        when(listSalonAppointmentsUseCase.execute(
                employeeId, startDate, endDate, AppointmentStatus.PENDING, 0, 20, principal
        )).thenReturn(expected);

        // Act
        ResponseEntity<PageResponse<AppointmentResponse>> response = appointmentController.getSalonAppointments(
                principal, employeeId, startDate, endDate, AppointmentStatus.PENDING, 0, 20
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(listSalonAppointmentsUseCase).execute(
                employeeId, startDate, endDate, AppointmentStatus.PENDING, 0, 20, principal
        );
    }

    private UserPrincipal createPrincipal(UUID userId) {
        return new UserPrincipal(
                userId,
                "cliente@perola.cv",
                "hash",
                List.of("CUSTOMER"),
                List.of(),
                true
        );
    }

    private AppointmentResponse createSampleResponse(
            UUID customerId,
            OffsetDateTime startTime,
            AppointmentStatus status
    ) {
        return new AppointmentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                customerId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Corte",
                new BigDecimal("1500.00"),
                45,
                startTime,
                startTime.plusMinutes(45),
                null,
                null,
                status,
                "Notas",
                startTime
        );
    }
}
