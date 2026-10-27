package cv.perola.modules.business.usecase;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchMarketplaceBusinessesUseCase {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessMapper businessMapper;

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceBusinessSummaryResponse> execute(
            String search,
            String island,
            String city,
            Pageable pageable) {

        String cleanSearch = normalizeFilter(search);
        String cleanIsland = normalizeFilter(island);
        String cleanCity = normalizeFilter(city);

        Page<BusinessEntity> businessPage = businessRepository.searchMarketplaceBusinesses(
                cleanSearch,
                cleanIsland,
                cleanCity,
                pageable
        );

        Page<MarketplaceBusinessSummaryResponse> responsePage = businessPage.map(business -> {
            long servicesCount = serviceRepository.countByBusinessIdAndIsActiveTrue(business.getId());
            return businessMapper.toSummaryResponse(business, servicesCount);
        });

        return PageResponse.from(responsePage);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
