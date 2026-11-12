package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessDetailResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
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
                .logoUrl(normalizeUrl(request.logoUrl()))
                .timezone(timezone)
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

    public BusinessResponse toResponse(BusinessEntity entity, List<BusinessCategoryResponse> categories) {
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
                entity.getLogoUrl(),
                entity.getTimezone(),
                categories != null ? categories : Collections.emptyList(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public MarketplaceBusinessSummaryResponse toSummaryResponse(
            BusinessEntity entity,
            List<BusinessCategoryResponse> categories,
            long servicesCount) {
        if (entity == null) {
            return null;
        }
        return new MarketplaceBusinessSummaryResponse(
                entity.getId(),
                entity.getName(),
                entity.getIsland(),
                entity.getCity(),
                entity.getLocation(),
                entity.getContactPhone(),
                entity.getLogoUrl(),
                entity.getTimezone(),
                categories != null ? categories : Collections.emptyList(),
                servicesCount);
    }

    public MarketplaceBusinessDetailResponse toDetailResponse(
            BusinessEntity entity,
            List<BusinessCategoryResponse> categories,
            List<ServiceResponse> services,
            List<EmployeeResponse> employees) {
        if (entity == null) {
            return null;
        }
        return new MarketplaceBusinessDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getIsland(),
                entity.getCity(),
                entity.getLocation(),
                entity.getContactPhone(),
                entity.getLogoUrl(),
                entity.getTimezone(),
                categories != null ? categories : Collections.emptyList(),
                services,
                employees);
    }
}
