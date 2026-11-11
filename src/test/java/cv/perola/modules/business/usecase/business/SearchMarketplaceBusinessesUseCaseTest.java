package cv.perola.modules.business.usecase.business;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.CategoryLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchMarketplaceBusinessesUseCaseTest {

        @Mock
        private BusinessRepository businessRepository;

        @Mock
        private ServiceRepository serviceRepository;

        @Mock
        private BusinessCategoryRepository businessCategoryRepository;

        @Mock
        private CategoryLookupService categoryLookupService;

        @Spy
        private BusinessMapper businessMapper = new BusinessMapper();

        @Spy
        private CategoryMapper categoryMapper = new CategoryMapper();

        @InjectMocks
        private SearchMarketplaceBusinessesUseCase useCase;

        @Test
        @DisplayName("Deve pesquisar salões sem categoria e retornar página com contagem de serviços")
        void shouldSearchBusinessesAndReturnPageWithServicesCount() {
                // Arrange
                UUID b1Id = UUID.randomUUID();
                BusinessEntity b1 = BusinessEntity.builder()
                                .ownerId(UUID.randomUUID())
                                .name("Salão Beleza Crioula")
                                .island("Santiago")
                                .city("Praia")
                                .location("Platô")
                                .contactPhone("+2382610000")
                                .timezone("Atlantic/Cape_Verde")
                                .isActive(true)
                                .build();
                b1.setId(b1Id);

                Pageable pageable = PageRequest.of(0, 10);
                when(businessRepository.searchMarketplaceBusinesses(eq("Beleza"), eq("Santiago"), eq("Praia"),
                                eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(b1), pageable, 1));
                when(businessCategoryRepository.findAllByBusinessIdIn(Set.of(b1Id))).thenReturn(List.of());
                when(serviceRepository.countByBusinessIdAndIsActiveTrue(b1Id)).thenReturn(5L);

                // Act
                PageResponse<MarketplaceBusinessSummaryResponse> response = useCase.execute(
                                " Beleza ", null, " Santiago ", " Praia ", pageable);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.totalElements()).isEqualTo(1);
                assertThat(response.content()).hasSize(1);
                MarketplaceBusinessSummaryResponse item = response.content().get(0);
                assertThat(item.id()).isEqualTo(b1Id);
                assertThat(item.name()).isEqualTo("Salão Beleza Crioula");
                assertThat(item.servicesCount()).isEqualTo(5L);

                verify(businessRepository).searchMarketplaceBusinesses("Beleza", "Santiago", "Praia", pageable);
        }

        @Test
        @DisplayName("Deve pesquisar salões filtrando por slug de categoria e incluir categorias na resposta")
        void shouldSearchBusinessesFilteredByCategorySlug() {
                // Arrange
                UUID b1Id = UUID.randomUUID();
                UUID catId = UUID.randomUUID();
                CategoryEntity category = CategoryEntity.builder()
                                .slug("barbershop")
                                .name("Barbearia")
                                .icon("Scissors")
                                .displayOrder(1)
                                .build();
                category.setId(catId);

                BusinessEntity b1 = BusinessEntity.builder()
                                .ownerId(UUID.randomUUID())
                                .name("Barbearia Atlântico")
                                .island("Santiago")
                                .city("Praia")
                                .location("Platô")
                                .contactPhone("+2382610000")
                                .timezone("Atlantic/Cape_Verde")
                                .isActive(true)
                                .build();
                b1.setId(b1Id);

                BusinessCategoryEntity bc = BusinessCategoryEntity.builder()
                                .businessId(b1Id)
                                .categoryId(catId)
                                .category(category)
                                .isPrimary(true)
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                when(categoryLookupService.findActiveBySlugOrId("barbershop")).thenReturn(Optional.of(category));
                when(businessRepository.searchMarketplaceBusinessesByCategory(null, null, null, catId, pageable))
                                .thenReturn(new PageImpl<>(List.of(b1), pageable, 1));
                when(businessCategoryRepository.findAllByBusinessIdIn(Set.of(b1Id))).thenReturn(List.of(bc));
                when(serviceRepository.countByBusinessIdAndIsActiveTrue(b1Id)).thenReturn(3L);

                // Act
                PageResponse<MarketplaceBusinessSummaryResponse> response = useCase.execute(
                                null, "barbershop", null, null, pageable);

                // Assert
                assertThat(response.content()).hasSize(1);
                MarketplaceBusinessSummaryResponse item = response.content().get(0);
                assertThat(item.categories()).hasSize(1);
                assertThat(item.categories().get(0).slug()).isEqualTo("barbershop");
                assertThat(item.categories().get(0).isPrimary()).isTrue();
        }

        @Test
        @DisplayName("Deve retornar página vazia quando filtro de categoria não corresponder a nenhuma categoria ativa")
        void shouldReturnEmptyPageWhenCategoryNotFound() {
                // Arrange
                Pageable pageable = PageRequest.of(0, 10);
                when(categoryLookupService.findActiveBySlugOrId("inexistente")).thenReturn(Optional.empty());

                // Act
                PageResponse<MarketplaceBusinessSummaryResponse> response = useCase.execute(
                                null, "inexistente", null, null, pageable);

                // Assert
                assertThat(response.content()).isEmpty();
                assertThat(response.totalElements()).isZero();
        }
}