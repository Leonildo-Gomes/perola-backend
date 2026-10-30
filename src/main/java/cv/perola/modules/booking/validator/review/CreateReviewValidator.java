package cv.perola.modules.booking.validator.review;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateReviewValidator {

    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;

    public AppointmentEntity validate(UUID appointmentId, UUID customerId, CreateReviewRequest request) {
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new ValidationException("A nota de avaliação (rating) deve estar compreendida entre 1 e 5.");
        }

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

        if (!appointment.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Apenas o cliente autor do agendamento pode submeter uma avaliação.");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ValidationException(
                    "A avaliação só pode ser submetida após a conclusão do atendimento (COMPLETED)."
            );
        }

        if (reviewRepository.existsByAppointmentId(appointmentId)) {
            throw new ConflictException("Já existe uma avaliação registada para este agendamento.");
        }

        return appointment;
    }
}
