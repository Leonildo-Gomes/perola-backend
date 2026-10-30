package cv.perola.modules.booking.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.mapper.ReviewMapper;
import cv.perola.modules.booking.repository.ReviewRepository;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListBusinessReviewsUseCase {

    private static final String DEFAULT_TIMEZONE = "Atlantic/Cape_Verde";
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> execute(UUID businessId, int page, int size) {
        BusinessResponse business = businessEmployeeQueryService.findBusinessById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Salão não encontrado."));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        ZoneId zoneId = resolveZoneId(business.timezone());
        Page<ReviewEntity> reviewsPage = reviewRepository.findAllByBusinessId(businessId, pageable);
        Page<ReviewResponse> responsePage = reviewsPage.map(entity -> reviewMapper.toResponse(entity, zoneId));

        return PageResponse.from(responsePage);
    }

    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception ignored) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }
}

