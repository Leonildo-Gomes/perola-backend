package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private CancelAppointmentUseCase cancelAppointmentUseCase;

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PENDING", "CONFIRMED", "RESCHEDULE_PENDING"})
    @DisplayName("Deve permitir cancelamento a partir de PENDING, CONFIRMED ou RESCHEDULE_PENDING")
    void shouldCancelAppointmentFromActiveStatuses(AppointmentStatus activeStatus) {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "cust@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true);

        Instant start = Instant.parse("2026-11-03T10:00:00Z");
        Instant end = Instant.parse("2026-11-03T11:00:00Z");
        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(UUID.randomUUID())
                .customerId(userId)
                .employeeId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .createdById(userId)
                .startTime(start)
                .endTime(end)
                .proposedStartTime(start.plusSeconds(3600))
                .proposedEndTime(end.plusSeconds(3600))
                .serviceName("Corte")
                .servicePrice(new BigDecimal("1500.00"))
                .serviceDuration(60)
                .status(activeStatus)
                .build();
        appointment.setId(appointmentId);

        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId, appointment.getBusinessId(), userId,
                appointment.getEmployeeId(), appointment.getServiceId(), "Corte",
                new BigDecimal("1500.00"), 60, OffsetDateTime.ofInstant(start, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(end, ZoneOffset.UTC), null, null,
                AppointmentStatus.CANCELLED, "Imprevisto", OffsetDateTime.now()
        );

        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);
        when(lifecycleService.recordTransitionAndBuildResponse(
                appointment, userId, AppointmentAction.CANCELLED, activeStatus,
                AppointmentStatus.CANCELLED, start, end, null, null, "Imprevisto"
        )).thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = cancelAppointmentUseCase.execute(appointmentId, "Imprevisto", principal);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getProposedStartTime()).isNull();
        assertThat(appointment.getProposedEndTime()).isNull();
        verify(lifecycleService).validateParticipantAccess(appointment, principal);
        verify(appointmentRepository).saveAndFlush(appointment);
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "COMPLETED"})
    @DisplayName("Deve rejeitar cancelamento quando o agendamento já estiver CANCELLED ou COMPLETED")
    void shouldThrowValidationExceptionWhenCancellingTerminalStatus(AppointmentStatus terminalStatus) {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "cust@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true);
        AppointmentEntity appointment = AppointmentEntity.builder()
                .status(terminalStatus)
                .build();
        appointment.setId(appointmentId);
        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);

        // Act & Assert
        assertThatThrownBy(() -> cancelAppointmentUseCase.execute(appointmentId, "Motivo", principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Não é possível cancelar um agendamento no estado");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }
}

