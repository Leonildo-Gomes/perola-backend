package cv.perola.modules.booking.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.mapper.ReviewMapper;
import cv.perola.modules.booking.repository.ReviewRepository;
import cv.perola.modules.business.dto.EmployeeResponse;
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
public class ListEmployeeReviewsUseCase {

    private static final String DEFAULT_TIMEZONE = "Atlantic/Cape_Verde";
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> execute(UUID employeeId, int page, int size) {
        EmployeeResponse employee = businessEmployeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        ZoneId zoneId = resolveZoneId(employee.businessId());
        Page<ReviewEntity> reviewsPage = reviewRepository.findAllByEmployeeIdOrderByCreatedAtDesc(
                employeeId, pageable
        );
        Page<ReviewResponse> responsePage = reviewsPage.map(entity -> reviewMapper.toResponse(entity, zoneId));

        return PageResponse.from(responsePage);
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

