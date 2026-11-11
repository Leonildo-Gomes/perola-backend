package cv.perola.modules.business.usecase.business;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.CategoryLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchMarketplaceBusinessesUseCase {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessCategoryRepository businessCategoryRepository;
    private final CategoryLookupService categoryLookupService;
    private final BusinessMapper businessMapper;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceBusinessSummaryResponse> execute(
            String search,
            String category,
            String island,
            String city,
            Pageable pageable) {

        String cleanSearch = normalizeFilter(search);
        String cleanCategory = normalizeFilter(category);
        String cleanIsland = normalizeFilter(island);
        String cleanCity = normalizeFilter(city);

        Page<BusinessEntity> businessPage;
        if (cleanCategory != null) {
            Optional<CategoryEntity> categoryEntity = categoryLookupService.findActiveBySlugOrId(cleanCategory);
            if (categoryEntity.isEmpty()) {
                return PageResponse.from(Page.empty(pageable));
            }
            businessPage = businessRepository.searchMarketplaceBusinessesByCategory(
                    cleanSearch,
                    cleanIsland,
                    cleanCity,
                    categoryEntity.get().getId(),
                    pageable);
        } else {
            businessPage = businessRepository.searchMarketplaceBusinesses(
                    cleanSearch,
                    cleanIsland,
                    cleanCity,
                    pageable);
        }

        Set<UUID> businessIds = businessPage.getContent().stream()
                .map(BusinessEntity::getId)
                .collect(Collectors.toSet());

        Map<UUID, List<BusinessCategoryEntity>> categoriesByBusiness = businessIds.isEmpty()
                ? Collections.emptyMap()
                : businessCategoryRepository.findAllByBusinessIdIn(businessIds).stream()
                        .collect(Collectors.groupingBy(BusinessCategoryEntity::getBusinessId));

        Page<MarketplaceBusinessSummaryResponse> responsePage = businessPage.map(business -> {
            long servicesCount = serviceRepository.countByBusinessIdAndIsActiveTrue(business.getId());
            List<BusinessCategoryResponse> categories = categoryMapper.toBusinessCategoryResponses(
                    categoriesByBusiness.getOrDefault(business.getId(), Collections.emptyList()));
            return businessMapper.toSummaryResponse(business, categories, servicesCount);
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
