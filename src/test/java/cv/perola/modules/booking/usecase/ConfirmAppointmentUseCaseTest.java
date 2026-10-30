package cv.perola.modules.booking.usecase;

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
class ConfirmAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private ConfirmAppointmentUseCase confirmAppointmentUseCase;

    @Test
    @DisplayName("Deve confirmar agendamento quando estiver no estado PENDING")
    void shouldConfirmAppointmentWhenStatusIsPending() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "owner@perola.cv", "hash", List.of("OWNER"), List.of(), true);

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
                .status(AppointmentStatus.PENDING)
                .build();
        appointment.setId(appointmentId);

        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId, appointment.getBusinessId(), appointment.getCustomerId(),
                appointment.getEmployeeId(), appointment.getServiceId(), "Corte",
                new BigDecimal("1500.00"), 60, OffsetDateTime.ofInstant(start, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(end, ZoneOffset.UTC), null, null,
                AppointmentStatus.CONFIRMED, "Confirmado", OffsetDateTime.now()
        );

        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);
        when(lifecycleService.recordTransitionAndBuildResponse(
                appointment, userId, AppointmentAction.CONFIRMED, AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED, null, null, start, end, "Confirmado pelo salão"
        )).thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = confirmAppointmentUseCase.execute(appointmentId, "Confirmado pelo salão", principal);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(lifecycleService).validateSalonManagementAccess(appointment, principal);
        verify(appointmentRepository).saveAndFlush(appointment);
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CONFIRMED", "RESCHEDULE_PENDING", "CANCELLED", "COMPLETED"})
    @DisplayName("Deve rejeitar confirmação quando o agendamento não estiver no estado PENDING")
    void shouldThrowValidationExceptionWhenStatusIsNotPending(AppointmentStatus invalidStatus) {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "hash", List.of("OWNER"), List.of(), true);
        AppointmentEntity appointment = AppointmentEntity.builder()
                .status(invalidStatus)
                .build();
        appointment.setId(appointmentId);
        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);

        // Act & Assert
        assertThatThrownBy(() -> confirmAppointmentUseCase.execute(appointmentId, null, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Apenas agendamentos no estado PENDING podem ser confirmados");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }
}

