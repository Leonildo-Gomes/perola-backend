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
 * Caso de uso para aceitação de proposta de reagendamento (RESCHEDULE_PENDING -> CONFIRMED),
 * atualizando start_time / end_time para o horário proposto e limpando os campos proposed_*.
 * Se o novo horário tiver sido ocupado entretanto, o PostgreSQL dispara erro GiST 23P01 (HTTP 409).
 */
@Service
@RequiredArgsConstructor
public class AcceptRescheduleUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public AppointmentResponse execute(UUID appointmentId, String notes, UserPrincipal principal) {
        AppointmentEntity appointment = lifecycleService.findAppointmentOrThrow(appointmentId);
        lifecycleService.validateParticipantAccess(appointment, principal);

        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus != AppointmentStatus.RESCHEDULE_PENDING
                || appointment.getProposedStartTime() == null
                || appointment.getProposedEndTime() == null) {
            throw new ValidationException(
                    "Apenas agendamentos no estado RESCHEDULE_PENDING com proposta ativa podem ser aceites. Estado atual: "
                            + previousStatus
            );
        }

        Instant oldStart = appointment.getStartTime();
        Instant oldEnd = appointment.getEndTime();
        Instant newStart = appointment.getProposedStartTime();
        Instant newEnd = appointment.getProposedEndTime();

        appointment.setStartTime(newStart);
        appointment.setEndTime(newEnd);
        appointment.setProposedStartTime(null);
        appointment.setProposedEndTime(null);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        return lifecycleService.recordTransitionAndBuildResponse(
                saved,
                principal.getId(),
                AppointmentAction.RESCHEDULE_CONFIRMED,
                previousStatus,
                AppointmentStatus.CONFIRMED,
                oldStart,
                oldEnd,
                newStart,
                newEnd,
                notes
        );
    }
}

