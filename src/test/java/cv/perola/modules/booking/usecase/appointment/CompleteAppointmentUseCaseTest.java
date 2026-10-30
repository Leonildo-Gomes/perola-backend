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
import org.junit.jupiter.api.Test;
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
class CompleteAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private CompleteAppointmentUseCase completeAppointmentUseCase;

    @Test
    @DisplayName("Deve concluir agendamento quando estiver no estado CONFIRMED")
    void shouldCompleteAppointmentWhenStatusIsConfirmed() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "emp@perola.cv", "hash", List.of("EMPLOYEE"), List.of(), true);

        Instant start = Instant.parse("2026-11-03T10:00:00Z");
        Instant end = Instant.parse("2026-11-03T11:00:00Z");
        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .createdById(UUID.randomUUID())
                .startTime(start)
                .endTime(end)
                .serviceName("Corte")
                .servicePrice(new BigDecimal("1500.00"))
                .serviceDuration(60)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        appointment.setId(appointmentId);

        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId, appointment.getBusinessId(), appointment.getCustomerId(),
                appointment.getEmployeeId(), appointment.getServiceId(), "Corte",
                new BigDecimal("1500.00"), 60, OffsetDateTime.ofInstant(start, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(end, ZoneOffset.UTC), null, null,
                AppointmentStatus.COMPLETED, "Atendimento finalizado", OffsetDateTime.now()
        );

        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);
        when(lifecycleService.recordTransitionAndBuildResponse(
                appointment, userId, AppointmentAction.COMPLETED, AppointmentStatus.CONFIRMED,
                AppointmentStatus.COMPLETED, start, end, null, null, "Atendimento finalizado"
        )).thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = completeAppointmentUseCase.execute(appointmentId, "Atendimento finalizado", principal);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(lifecycleService).validateSalonManagementAccess(appointment, principal);
        verify(appointmentRepository).saveAndFlush(appointment);
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PENDING", "RESCHEDULE_PENDING", "CANCELLED", "COMPLETED"})
    @DisplayName("Deve rejeitar conclusão quando o agendamento não estiver no estado CONFIRMED")
    void shouldThrowValidationExceptionWhenStatusIsNotConfirmed(AppointmentStatus invalidStatus) {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "emp@perola.cv", "hash", List.of("EMPLOYEE"), List.of(), true);
        AppointmentEntity appointment = AppointmentEntity.builder()
                .status(invalidStatus)
                .build();
        appointment.setId(appointmentId);
        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);

        // Act & Assert
        assertThatThrownBy(() -> completeAppointmentUseCase.execute(appointmentId, null, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Apenas agendamentos no estado CONFIRMED podem ser concluídos");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }
}

