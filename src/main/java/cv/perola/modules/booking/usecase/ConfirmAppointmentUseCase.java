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

import java.util.UUID;

/**
 * Caso de uso para confirmação de um agendamento pendente (PENDING -> CONFIRMED).
 */
@Service
@RequiredArgsConstructor
public class ConfirmAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public AppointmentResponse execute(UUID appointmentId, String notes, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateSalonManagementAccess(appointment, principal);

        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus != AppointmentStatus.PENDING) {
            throw new ValidationException(
                    "Apenas agendamentos no estado PENDING podem ser confirmados. Estado atual: " + previousStatus
            );
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        return lifecycleService.recordTransitionAndBuildResponse(
                saved,
                principal.getId(),
                AppointmentAction.CONFIRMED,
                previousStatus,
                AppointmentStatus.CONFIRMED,
                null,
                null,
                saved.getStartTime(),
                saved.getEndTime(),
                notes
        );
    }
}

