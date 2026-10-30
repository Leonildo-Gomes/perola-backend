package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.RescheduleAppointmentRequest;
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
class ProposeRescheduleUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private ProposeRescheduleUseCase proposeRescheduleUseCase;

    @Test
    @DisplayName("Deve propor reagendamento preservando start_time original e transitando para RESCHEDULE_PENDING")
    void shouldProposeReschedulePreservingOriginalTimesAndSettingProposedTimes() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "cust@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true);

        Instant originalStart = Instant.parse("2026-11-03T10:00:00Z");
        Instant originalEnd = Instant.parse("2026-11-03T11:00:00Z");
        OffsetDateTime proposedStartOffset = OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.ofHours(-1));
        Instant expectedProposedStart = Instant.parse("2026-11-05T15:00:00Z");
        Instant expectedProposedEnd = Instant.parse("2026-11-05T16:00:00Z");

        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(UUID.randomUUID())
                .customerId(userId)
                .employeeId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .createdById(userId)
                .startTime(originalStart)
                .endTime(originalEnd)
                .serviceName("Corte")
                .servicePrice(new BigDecimal("1500.00"))
                .serviceDuration(60)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        appointment.setId(appointmentId);

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(proposedStartOffset, "Viagem de trabalho");

        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId, appointment.getBusinessId(), userId,
                appointment.getEmployeeId(), appointment.getServiceId(), "Corte",
                new BigDecimal("1500.00"), 60,
                OffsetDateTime.ofInstant(originalStart, ZoneOffset.ofHours(-1)),
                OffsetDateTime.ofInstant(originalEnd, ZoneOffset.ofHours(-1)),
                proposedStartOffset,
                proposedStartOffset.plusMinutes(60),
                AppointmentStatus.RESCHEDULE_PENDING, "Viagem de trabalho", OffsetDateTime.now()
        );

        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);
        when(lifecycleService.recordTransitionAndBuildResponse(
                appointment, userId, AppointmentAction.RESCHEDULE_REQUESTED,
                AppointmentStatus.CONFIRMED, AppointmentStatus.RESCHEDULE_PENDING,
                originalStart, originalEnd, expectedProposedStart, expectedProposedEnd, "Viagem de trabalho"
        )).thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = proposeRescheduleUseCase.execute(appointmentId, request, principal);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.RESCHEDULE_PENDING);
        assertThat(appointment.getStartTime()).isEqualTo(originalStart);
        assertThat(appointment.getEndTime()).isEqualTo(originalEnd);
        assertThat(appointment.getProposedStartTime()).isEqualTo(expectedProposedStart);
        assertThat(appointment.getProposedEndTime()).isEqualTo(expectedProposedEnd);
        verify(appointmentRepository).saveAndFlush(appointment);
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PENDING", "RESCHEDULE_PENDING", "CANCELLED", "COMPLETED"})
    @DisplayName("Deve rejeitar proposta de reagendamento quando o estado atual não for CONFIRMED")
    void shouldThrowValidationExceptionWhenStatusIsNotConfirmed(AppointmentStatus invalidStatus) {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "cust@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true);
        AppointmentEntity appointment = AppointmentEntity.builder()
                .status(invalidStatus)
                .build();
        appointment.setId(appointmentId);
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                OffsetDateTime.now(ZoneOffset.ofHours(-1)), "Motivo"
        );
        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);

        // Act & Assert
        assertThatThrownBy(() -> proposeRescheduleUseCase.execute(appointmentId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Apenas agendamentos no estado CONFIRMED podem solicitar reagendamento");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }
}

