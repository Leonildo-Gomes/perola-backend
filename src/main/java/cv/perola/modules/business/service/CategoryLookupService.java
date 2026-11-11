package cv.perola.modules.business.service;

import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryLookupService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Optional<CategoryEntity> findActiveBySlugOrId(String categoryFilter) {
        if (categoryFilter == null || categoryFilter.trim().isBlank()) {
            return Optional.empty();
        }
        String normalized = categoryFilter.trim();
        try {
            UUID categoryId = UUID.fromString(normalized);
            return categoryRepository.findByIdAndIsActiveTrue(categoryId);
        } catch (IllegalArgumentException ignored) {
            return categoryRepository.findBySlugAndIsActiveTrue(normalized.toLowerCase(Locale.ROOT));
        }
    }
}

