package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BusinessMapper {

    private static final String DEFAULT_TIMEZONE = "Atlantic/Cape_Verde";

    public BusinessEntity toEntity(CreateBusinessRequest request, UUID ownerId) {
        if (request == null) {
            return null;
        }

        String timezone = (request.timezone() != null && !request.timezone().isBlank())
                ? request.timezone().trim()
                : DEFAULT_TIMEZONE;

        return BusinessEntity.builder()
                .ownerId(ownerId)
                .name(request.name() != null ? request.name().trim() : null)
                .island(request.island() != null ? request.island().trim() : null)
                .city(request.city() != null ? request.city().trim() : null)
                .location(request.location() != null ? request.location().trim() : null)
                .contactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null)
                .timezone(timezone)
                .isActive(true)
                .build();
    }

    public BusinessResponse toResponse(BusinessEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BusinessResponse(
                entity.getId(),
                entity.getOwnerId(),
                entity.getName(),
                entity.getIsland(),
                entity.getCity(),
                entity.getLocation(),
                entity.getContactPhone(),
                entity.getTimezone(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
