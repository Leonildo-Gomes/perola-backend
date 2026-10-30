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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso para propor reagendamento (CONFIRMED -> RESCHEDULE_PENDING),
 * mantendo o horário original bloqueado e preenchendo proposed_start_time / proposed_end_time.
 */
@Service
@RequiredArgsConstructor
public class ProposeRescheduleUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public AppointmentResponse execute(UUID appointmentId, RescheduleAppointmentRequest request, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateParticipantAccess(appointment, principal);

        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus != AppointmentStatus.CONFIRMED) {
            throw new ValidationException(
                    "Apenas agendamentos no estado CONFIRMED podem solicitar reagendamento. Estado atual: " + previousStatus
            );
        }

        Instant proposedStart = request.proposedStartTime().toInstant();
        Instant proposedEnd = proposedStart.plus(Duration.ofMinutes(appointment.getServiceDuration()));

        appointment.setProposedStartTime(proposedStart);
        appointment.setProposedEndTime(proposedEnd);
        appointment.setStatus(AppointmentStatus.RESCHEDULE_PENDING);
        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        return lifecycleService.recordTransitionAndBuildResponse(
                saved,
                principal.getId(),
                AppointmentAction.RESCHEDULE_REQUESTED,
                previousStatus,
                AppointmentStatus.RESCHEDULE_PENDING,
                saved.getStartTime(),
                saved.getEndTime(),
                proposedStart,
                proposedEnd,
                request.reason()
        );
    }
}

