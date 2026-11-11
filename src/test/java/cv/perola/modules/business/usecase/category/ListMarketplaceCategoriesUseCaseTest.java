package cv.perola.modules.business.usecase.category;

import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.CategoryResponse;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMarketplaceCategoriesUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper();

    @InjectMocks
    private ListMarketplaceCategoriesUseCase useCase;

    @Test
    @DisplayName("Deve listar categorias ativas ordenadas por displayOrder")
    void shouldListActiveCategoriesOrdered() {
        // Arrange
        CategoryEntity c1 = CategoryEntity.builder().slug("barbershop").name("Barbearia").icon("Scissors").displayOrder(1).isActive(true).build();
        c1.setId(UUID.randomUUID());
        CategoryEntity c2 = CategoryEntity.builder().slug("hair").name("Cabeleireiro & Tranças").icon("Sparkles").displayOrder(2).isActive(true).build();
        c2.setId(UUID.randomUUID());

        when(categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(c1, c2));

        // Act
        List<CategoryResponse> result = useCase.execute();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).slug()).isEqualTo("barbershop");
        assertThat(result.get(1).slug()).isEqualTo("hair");
        verify(categoryRepository).findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }
}

