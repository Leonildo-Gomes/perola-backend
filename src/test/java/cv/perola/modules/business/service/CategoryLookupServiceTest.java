package cv.perola.modules.business.service;

import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryLookupServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryLookupService service;

    @Test
    @DisplayName("Deve resolver categoria por UUID quando o filtro for UUID válido")
    void shouldResolveByUuid() {
        // Arrange
        UUID id = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("barbershop").name("Barbearia").build();
        category.setId(id);
        when(categoryRepository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.of(category));

        // Act
        Optional<CategoryEntity> result = service.findActiveBySlugOrId(id.toString());

        // Assert
        assertThat(result).contains(category);
        verify(categoryRepository).findByIdAndIsActiveTrue(id);
    }

    @Test
    @DisplayName("Deve resolver categoria por slug normalizado em minúsculas")
    void shouldResolveBySlug() {
        // Arrange
        CategoryEntity category = CategoryEntity.builder().slug("spa_massage").name("Spa & Massagem").build();
        when(categoryRepository.findBySlugAndIsActiveTrue("spa_massage")).thenReturn(Optional.of(category));

        // Act
        Optional<CategoryEntity> result = service.findActiveBySlugOrId("  SPA_MASSAGE ");

        // Assert
        assertThat(result).contains(category);
        verify(categoryRepository).findBySlugAndIsActiveTrue("spa_massage");
    }

    @Test
    @DisplayName("Deve retornar vazio quando filtro for nulo ou em branco")
    void shouldReturnEmptyWhenFilterIsBlank() {
        // Arrange & Act
        Optional<CategoryEntity> result = service.findActiveBySlugOrId("   ");

        // Assert
        assertThat(result).isEmpty();
    }
}

