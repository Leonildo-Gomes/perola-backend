package cv.perola.modules.business.validator.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessCategoryValidatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BusinessCategoryValidator validator;

    @Test
    @DisplayName("Deve validar e resolver múltiplas categorias quando exatamente 1 for principal")
    void shouldValidateMixedSpaceCategoriesWithSinglePrimary() {
        // Arrange
        UUID cat1Id = UUID.randomUUID();
        UUID cat2Id = UUID.randomUUID();
        CategoryEntity cat1 = CategoryEntity.builder().slug("hair").name("Cabeleireiro").isActive(true).build();
        cat1.setId(cat1Id);
        CategoryEntity cat2 = CategoryEntity.builder().slug("nails").name("Unhas").isActive(true).build();
        cat2.setId(cat2Id);

        List<BusinessCategoryItemRequest> items = List.of(
                new BusinessCategoryItemRequest(cat1Id, true),
                new BusinessCategoryItemRequest(cat2Id, false)
        );
        when(categoryRepository.findAllByIdInAndIsActiveTrue(Set.of(cat1Id, cat2Id)))
                .thenReturn(List.of(cat1, cat2));

        // Act
        Map<UUID, CategoryEntity> result = validator.validateAndResolveCategories(items);

        // Assert
        assertThat(result).containsKeys(cat1Id, cat2Id);
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando lista de categorias estiver vazia")
    void shouldThrowValidationExceptionWhenCategoriesListIsEmpty() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> validator.validateAndResolveCategories(List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("pelo menos uma categoria");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando existirem 2 categorias com isPrimary = true")
    void shouldThrowValidationExceptionWhenMultiplePrimaryCategories() {
        // Arrange
        List<BusinessCategoryItemRequest> items = List.of(
                new BusinessCategoryItemRequest(UUID.randomUUID(), true),
                new BusinessCategoryItemRequest(UUID.randomUUID(), true)
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validateAndResolveCategories(items))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exatamente 1 categoria principal");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando nenhuma categoria tiver isPrimary = true")
    void shouldThrowValidationExceptionWhenNoPrimaryCategory() {
        // Arrange
        List<BusinessCategoryItemRequest> items = List.of(
                new BusinessCategoryItemRequest(UUID.randomUUID(), false)
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validateAndResolveCategories(items))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exatamente 1 categoria principal");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando houver categoryId duplicado")
    void shouldThrowValidationExceptionWhenDuplicateCategoryId() {
        // Arrange
        UUID duplicateId = UUID.randomUUID();
        List<BusinessCategoryItemRequest> items = List.of(
                new BusinessCategoryItemRequest(duplicateId, true),
                new BusinessCategoryItemRequest(duplicateId, false)
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validateAndResolveCategories(items))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("repetir categorias");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando alguma categoria não existir ou estiver inativa")
    void shouldThrowResourceNotFoundWhenCategoryDoesNotExist() {
        // Arrange
        UUID catId = UUID.randomUUID();
        List<BusinessCategoryItemRequest> items = List.of(
                new BusinessCategoryItemRequest(catId, true)
        );
        when(categoryRepository.findAllByIdInAndIsActiveTrue(Set.of(catId))).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> validator.validateAndResolveCategories(items))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não existem ou estão inativas");
    }
}

