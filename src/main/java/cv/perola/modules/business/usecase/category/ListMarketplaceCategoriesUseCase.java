package cv.perola.modules.business.usecase.category;

import cv.perola.modules.business.dto.CategoryResponse;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMarketplaceCategoriesUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> execute() {
        return categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
}

