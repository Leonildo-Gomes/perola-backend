package cv.perola.modules.booking.usecase;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso para rejeição de proposta de reagendamento (RESCHEDULE_PENDING -> CONFIRMED),
 * preservando o horário original garantido e limpando proposed_start_time / proposed_end_time.
 */
@Service
@RequiredArgsConstructor
public class RejectRescheduleUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public AppointmentResponse execute(UUID appointmentId, String reason, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateParticipantAccess(appointment, principal);

        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus != AppointmentStatus.RESCHEDULE_PENDING) {
            throw new ValidationException(
                    "Apenas agendamentos no estado RESCHEDULE_PENDING podem rejeitar reagendamento. Estado atual: "
                            + previousStatus
            );
        }

        Instant rejectedProposedStart = appointment.getProposedStartTime();
        Instant rejectedProposedEnd = appointment.getProposedEndTime();

        appointment.setProposedStartTime(null);
        appointment.setProposedEndTime(null);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        return lifecycleService.recordTransitionAndBuildResponse(
                saved,
                principal.getId(),
                AppointmentAction.RESCHEDULE_REJECTED,
                previousStatus,
                AppointmentStatus.CONFIRMED,
                saved.getStartTime(),
                saved.getEndTime(),
                rejectedProposedStart,
                rejectedProposedEnd,
                reason
        );
    }
}

