package cv.perola.modules.booking.usecase.review;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.mapper.ReviewMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.ReviewRepository;
import cv.perola.modules.booking.validator.review.CreateReviewValidator;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateReviewUseCase {

    private static final String DEFAULT_TIMEZONE = "Atlantic/Cape_Verde";

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final CreateReviewValidator createReviewValidator;

    @Transactional
    public ReviewResponse execute(UUID appointmentId, UUID customerId, CreateReviewRequest request) {
        AppointmentEntity appointment = createReviewValidator.validate(appointmentId, customerId, request);

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

