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
class RejectRescheduleUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private RejectRescheduleUseCase rejectRescheduleUseCase;

    @Test
    @DisplayName("Deve recusar proposta de reagendamento mantendo start_time original e regressando a CONFIRMED")
    void shouldRejectRescheduleKeepingOriginalTimesAndRestoringConfirmedStatus() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "owner@perola.cv", "hash", List.of("OWNER"), List.of(), true);

        Instant originalStart = Instant.parse("2026-11-03T10:00:00Z");
        Instant originalEnd = Instant.parse("2026-11-03T11:00:00Z");
        Instant proposedStart = Instant.parse("2026-11-05T15:00:00Z");
        Instant proposedEnd = Instant.parse("2026-11-05T16:00:00Z");

        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .createdById(UUID.randomUUID())
                .startTime(originalStart)
                .endTime(originalEnd)
                .proposedStartTime(proposedStart)
                .proposedEndTime(proposedEnd)
                .serviceName("Corte")
                .servicePrice(new BigDecimal("1500.00"))
                .serviceDuration(60)
                .status(AppointmentStatus.RESCHEDULE_PENDING)
                .build();
        appointment.setId(appointmentId);

        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId, appointment.getBusinessId(), appointment.getCustomerId(),
                appointment.getEmployeeId(), appointment.getServiceId(), "Corte",
                new BigDecimal("1500.00"), 60,
                OffsetDateTime.ofInstant(originalStart, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(originalEnd, ZoneOffset.UTC),
                null, null, AppointmentStatus.CONFIRMED, "Sem vaga", OffsetDateTime.now()
        );

        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);
        when(lifecycleService.recordTransitionAndBuildResponse(
                appointment, userId, AppointmentAction.RESCHEDULE_REJECTED,
                AppointmentStatus.RESCHEDULE_PENDING, AppointmentStatus.CONFIRMED,
                originalStart, originalEnd, proposedStart, proposedEnd, "Sem vaga no novo horário"
        )).thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = rejectRescheduleUseCase.execute(appointmentId, "Sem vaga no novo horário", principal);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getStartTime()).isEqualTo(originalStart);
        assertThat(appointment.getEndTime()).isEqualTo(originalEnd);
        assertThat(appointment.getProposedStartTime()).isNull();
        assertThat(appointment.getProposedEndTime()).isNull();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentRepository).saveAndFlush(appointment);
    }

    @Test
    @DisplayName("Deve lançar ValidationException ao recusar reagendamento quando estado não for RESCHEDULE_PENDING")
    void shouldThrowValidationExceptionWhenStatusIsNotReschedulePending() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "hash", List.of("OWNER"), List.of(), true);
        AppointmentEntity appointment = AppointmentEntity.builder()
                .status(AppointmentStatus.CONFIRMED)
                .build();
        appointment.setId(appointmentId);
        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);

        // Act & Assert
        assertThatThrownBy(() -> rejectRescheduleUseCase.execute(appointmentId, "Motivo", principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("RESCHEDULE_PENDING");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }
}

