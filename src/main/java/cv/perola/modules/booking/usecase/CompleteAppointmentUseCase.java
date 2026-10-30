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
 * Caso de uso para conclusão de um agendamento confirmado (CONFIRMED -> COMPLETED).
 */
@Service
@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public AppointmentResponse execute(UUID appointmentId, String notes, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateSalonManagementAccess(appointment, principal);

        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus != AppointmentStatus.CONFIRMED) {
            throw new ValidationException(
                    "Apenas agendamentos no estado CONFIRMED podem ser concluídos. Estado atual: " + previousStatus
            );
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        return lifecycleService.recordTransitionAndBuildResponse(
                saved,
                principal.getId(),
                AppointmentAction.COMPLETED,
                previousStatus,
                AppointmentStatus.COMPLETED,
                saved.getStartTime(),
                saved.getEndTime(),
                null,
                null,
                notes
        );
    }
}

