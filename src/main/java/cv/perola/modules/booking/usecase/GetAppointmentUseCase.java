package cv.perola.modules.booking.usecase;

import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso para consulta detalhada de um agendamento e respetiva linha do tempo de auditoria.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAppointmentUseCase {

    private final AppointmentLifecycleService lifecycleService;

    public AppointmentResponse execute(UUID appointmentId, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateParticipantAccess(appointment, principal);
        return lifecycleService.buildResponseWithHistory(appointment);
    }
}

