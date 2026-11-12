package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.MarketplaceServiceResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ServiceMapper {

    public ServiceEntity toEntity(CreateServiceRequest request, UUID businessId, CategoryEntity category) {
        return ServiceEntity.builder()
                .businessId(businessId)
                .category(category)
                .name(request.name() != null ? request.name().trim() : null)
                .description(request.description() != null ? request.description().trim() : null)
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .imageUrl(normalizeUrl(request.imageUrl()))
                .isActive(true)
                .build();
    }

    public String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public ServiceResponse toResponse(ServiceEntity entity) {
        return toResponse(entity, List.of());
    }

    public ServiceResponse toResponse(ServiceEntity entity, List<UUID> assignedEmployeeIds) {
        if (entity == null) {
            return null;
        }
        CategoryEntity category = entity.getCategory();
        List<UUID> safeAssignedIds = assignedEmployeeIds != null ? List.copyOf(assignedEmployeeIds) : List.of();
        return new ServiceResponse(
                entity.getId(),
                entity.getBusinessId(),
                category != null ? category.getId() : null,
                category != null ? category.getSlug() : null,
                category != null ? category.getName() : null,
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.isActive(),
                safeAssignedIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public MarketplaceServiceResponse toMarketplaceResponse(
            ServiceEntity entity,
            BusinessEntity business) {
        if (entity == null) {
            return null;
        }
        CategoryEntity category = entity.getCategory();
        return new MarketplaceServiceResponse(
                entity.getId(),
                entity.getBusinessId(),
                category != null ? category.getId() : null,
                category != null ? category.getSlug() : null,
                category != null ? category.getName() : null,
                business != null ? business.getName() : null,
                business != null ? business.getIsland() : null,
                business != null ? business.getCity() : null,
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getPrice(),
                entity.getImageUrl()
        );
    }
}
