package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.CategoryResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CategoryResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getName(),
                entity.getIcon(),
                entity.getDisplayOrder()
        );
    }

    public BusinessCategoryResponse toBusinessCategoryResponse(BusinessCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        CategoryEntity category = entity.getCategory();
        return new BusinessCategoryResponse(
                entity.getCategoryId(),
                category != null ? category.getSlug() : null,
                category != null ? category.getName() : null,
                category != null ? category.getIcon() : null,
                entity.isPrimary()
        );
    }

    public List<BusinessCategoryResponse> toBusinessCategoryResponses(List<BusinessCategoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .sorted(Comparator
                        .comparing(BusinessCategoryEntity::isPrimary).reversed()
                        .thenComparing(bc -> bc.getCategory() != null && bc.getCategory().getDisplayOrder() != null
                                ? bc.getCategory().getDisplayOrder()
                                : Integer.MAX_VALUE))
                .map(this::toBusinessCategoryResponse)
                .toList();
    }

    public List<BusinessCategoryEntity> toBusinessCategoryEntities(
            UUID businessId,
            List<BusinessCategoryItemRequest> items,
            Map<UUID, CategoryEntity> categoryMap) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> BusinessCategoryEntity.builder()
                        .businessId(businessId)
                        .categoryId(item.categoryId())
                        .category(categoryMap.get(item.categoryId()))
                        .isPrimary(Boolean.TRUE.equals(item.isPrimary()))
                        .build())
                .toList();
    }
}

