package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ServiceMapper {

    public ServiceEntity toEntity(CreateServiceRequest request, UUID businessId) {
        return ServiceEntity.builder()
                .businessId(businessId)
                .name(request.name() != null ? request.name().trim() : null)
                .description(request.description() != null ? request.description().trim() : null)
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .isActive(true)
                .build();
    }

    public ServiceResponse toResponse(ServiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ServiceResponse(
                entity.getId(),
                entity.getBusinessId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getPrice(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public cv.perola.modules.business.dto.MarketplaceServiceResponse toMarketplaceResponse(
            ServiceEntity entity,
            cv.perola.modules.business.domain.BusinessEntity business) {
        if (entity == null) {
            return null;
        }
        return new cv.perola.modules.business.dto.MarketplaceServiceResponse(
                entity.getId(),
                entity.getBusinessId(),
                business != null ? business.getName() : null,
                business != null ? business.getIsland() : null,
                business != null ? business.getCity() : null,
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getPrice()
        );
    }
}
