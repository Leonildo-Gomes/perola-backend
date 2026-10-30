package cv.perola.modules.booking.usecase;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.mapper.ReviewMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.ReviewRepository;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateReviewUseCase {

    private static final String DEFAULT_TIMEZONE = "Atlantic/Cape_Verde";

    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;

    @Transactional
    public ReviewResponse execute(UUID appointmentId, UUID customerId, CreateReviewRequest request) {
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

        ReviewEntity entity = reviewMapper.toEntity(appointment, request);
        ReviewEntity saved = reviewRepository.saveAndFlush(entity);

        ZoneId zoneId = resolveZoneId(appointment.getBusinessId());
        return reviewMapper.toResponse(saved, zoneId);
    }

    private ZoneId resolveZoneId(UUID businessId) {
        return businessEmployeeQueryService.findBusinessById(businessId)
                .map(business -> {
                    try {
                        return ZoneId.of(business.timezone());
                    } catch (Exception ignored) {
                        return ZoneId.of(DEFAULT_TIMEZONE);
                    }
                })
                .orElse(ZoneId.of(DEFAULT_TIMEZONE));
    }
}

