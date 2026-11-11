package cv.perola.modules.business.validator.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BusinessCategoryValidator {

    private final CategoryRepository categoryRepository;

    public Map<UUID, CategoryEntity> validateAndResolveCategories(List<BusinessCategoryItemRequest> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new ValidationException("Deve selecionar pelo menos uma categoria para o estabelecimento.");
        }

        Set<UUID> categoryIds = new HashSet<>();
        int primaryCount = 0;

        for (BusinessCategoryItemRequest item : categories) {
            if (item == null || item.categoryId() == null) {
                throw new ValidationException("O identificador da categoria é obrigatório.");
            }
            if (!categoryIds.add(item.categoryId())) {
                throw new ValidationException("Não é permitido repetir categorias no mesmo estabelecimento.");
            }
            if (Boolean.TRUE.equals(item.isPrimary())) {
                primaryCount++;
            }
        }

        if (primaryCount != 1) {
            throw new ValidationException("Deve definir exatamente 1 categoria principal (isPrimary = true).");
        }

        List<CategoryEntity> activeCategories = categoryRepository.findAllByIdInAndIsActiveTrue(categoryIds);
        if (activeCategories.size() != categoryIds.size()) {
            throw new ResourceNotFoundException("Uma ou mais categorias informadas não existem ou estão inativas.");
        }

        return activeCategories.stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
    }
}

