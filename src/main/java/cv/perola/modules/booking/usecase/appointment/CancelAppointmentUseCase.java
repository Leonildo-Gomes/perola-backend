package cv.perola.modules.booking.usecase.appointment;

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

import java.util.UUID;

/**
 * Caso de uso para cancelamento de um agendamento (PENDING | CONFIRMED | RESCHEDULE_PENDING -> CANCELLED).
 */
@Service
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public AppointmentResponse execute(UUID appointmentId, String reason, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateParticipantAccess(appointment, principal);

        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus == AppointmentStatus.CANCELLED || previousStatus == AppointmentStatus.COMPLETED) {
            throw new ValidationException(
                    "Não é possível cancelar um agendamento no estado " + previousStatus
            );
        }

        appointment.setProposedStartTime(null);
        appointment.setProposedEndTime(null);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        return lifecycleService.recordTransitionAndBuildResponse(
                saved,
                principal.getId(),
                AppointmentAction.CANCELLED,
                previousStatus,
                AppointmentStatus.CANCELLED,
                saved.getStartTime(),
                saved.getEndTime(),
                null,
                null,
                reason
        );
    }
}

