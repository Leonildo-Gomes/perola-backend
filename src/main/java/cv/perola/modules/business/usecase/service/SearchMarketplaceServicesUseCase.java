package cv.perola.modules.business.usecase.service;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.MarketplaceServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.CategoryLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchMarketplaceServicesUseCase {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final CategoryLookupService categoryLookupService;
    private final ServiceMapper serviceMapper;

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceServiceResponse> execute(
            String search,
            String category,
            String island,
            String city,
            Pageable pageable) {

        String cleanSearch = normalizeFilter(search);
        String cleanCategory = normalizeFilter(category);
        String cleanIsland = normalizeFilter(island);
        String cleanCity = normalizeFilter(city);

        Page<ServiceEntity> servicesPage;
        if (cleanCategory != null) {
            Optional<CategoryEntity> categoryEntity = categoryLookupService.findActiveBySlugOrId(cleanCategory);
            if (categoryEntity.isEmpty()) {
                return PageResponse.from(Page.empty(pageable));
            }
            servicesPage = serviceRepository.searchMarketplaceServicesByCategory(
                    cleanSearch,
                    cleanIsland,
                    cleanCity,
                    categoryEntity.get().getId(),
                    pageable);
        } else {
            servicesPage = serviceRepository.searchMarketplaceServices(
                    cleanSearch,
                    cleanIsland,
                    cleanCity,
                    pageable);
        }

        Set<UUID> businessIds = servicesPage.getContent().stream()
                .map(ServiceEntity::getBusinessId)
                .collect(Collectors.toSet());

        Map<UUID, BusinessEntity> businessesMap = businessRepository.findAllById(businessIds).stream()
                .collect(Collectors.toMap(BusinessEntity::getId, Function.identity()));

        Page<MarketplaceServiceResponse> responsePage = servicesPage.map(service -> {
            BusinessEntity business = businessesMap.get(service.getBusinessId());
            return serviceMapper.toMarketplaceResponse(service, business);
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
