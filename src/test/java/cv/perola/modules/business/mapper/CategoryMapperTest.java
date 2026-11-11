package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.CategoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper categoryMapper = new CategoryMapper();

    @Test
    @DisplayName("Deve converter CategoryEntity para CategoryResponse com sucesso")
    void shouldMapCategoryEntityToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        CategoryEntity entity = CategoryEntity.builder()
                .slug("barbershop")
                .name("Barbearia")
                .icon("Scissors")
                .displayOrder(1)
                .isActive(true)
                .build();
        entity.setId(id);

        // Act
        CategoryResponse response = categoryMapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.slug()).isEqualTo("barbershop");
        assertThat(response.name()).isEqualTo("Barbearia");
        assertThat(response.icon()).isEqualTo("Scissors");
        assertThat(response.displayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve ordenar categorias do estabelecimento com a principal (isPrimary=true) em primeiro lugar")
    void shouldSortPrimaryCategoryFirst() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID secondaryId = UUID.randomUUID();
        UUID primaryId = UUID.randomUUID();

        CategoryEntity secondaryCat = CategoryEntity.builder()
                .slug("barbershop").name("Barbearia").icon("Scissors").displayOrder(1).build();
        secondaryCat.setId(secondaryId);

        CategoryEntity primaryCat = CategoryEntity.builder()
                .slug("spa_massage").name("Spa & Massagem").icon("Flower2").displayOrder(4).build();
        primaryCat.setId(primaryId);

        BusinessCategoryEntity secondary = BusinessCategoryEntity.builder()
                .businessId(businessId).categoryId(secondaryId).category(secondaryCat).isPrimary(false).build();
        BusinessCategoryEntity primary = BusinessCategoryEntity.builder()
                .businessId(businessId).categoryId(primaryId).category(primaryCat).isPrimary(true).build();

        // Act
        List<BusinessCategoryResponse> responses = categoryMapper.toBusinessCategoryResponses(List.of(secondary, primary));

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(primaryId);
        assertThat(responses.get(0).isPrimary()).isTrue();
        assertThat(responses.get(1).id()).isEqualTo(secondaryId);
        assertThat(responses.get(1).isPrimary()).isFalse();
    }

    @Test
    @DisplayName("Deve converter itens de pedido para entidades BusinessCategoryEntity")
    void shouldMapItemsToBusinessCategoryEntities() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("nails").name("Unhas").build();
        category.setId(catId);

        List<BusinessCategoryItemRequest> items = List.of(new BusinessCategoryItemRequest(catId, true));

        // Act
        List<BusinessCategoryEntity> entities = categoryMapper.toBusinessCategoryEntities(
                businessId, items, Map.of(catId, category)
        );

        // Assert
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getBusinessId()).isEqualTo(businessId);
        assertThat(entities.get(0).getCategoryId()).isEqualTo(catId);
        assertThat(entities.get(0).getCategory()).isEqualTo(category);
        assertThat(entities.get(0).isPrimary()).isTrue();
    }
}

