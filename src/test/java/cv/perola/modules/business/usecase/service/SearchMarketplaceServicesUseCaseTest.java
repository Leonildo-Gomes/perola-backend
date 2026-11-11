package cv.perola.modules.business.usecase.service;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.MarketplaceServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchMarketplaceServicesUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private CategoryLookupService categoryLookupService;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private SearchMarketplaceServicesUseCase useCase;

    @Test
    @DisplayName("Deve pesquisar serviços sem categoria e preencher dados do salão detentor")
    void shouldSearchServicesAndPopulateBusinessDetails() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder()
                .slug("spa_massage")
                .name("Spa & Massagem")
                .build();
        category.setId(categoryId);

        BusinessEntity business = BusinessEntity.builder()
                .ownerId(UUID.randomUUID())
                .name("Salão Visual")
                .island("Sal")
                .city("Espargos")
                .location("Centro")
                .contactPhone("+2382421100")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        business.setId(businessId);

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .category(category)
                .name("Massagem Relaxante")
                .description("Sessão de relaxamento com óleos essenciais")
                .durationMinutes(50)
                .price(new BigDecimal("3500.00"))
                .isActive(true)
                .build();
        service.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        when(serviceRepository.searchMarketplaceServices(eq("Massagem"), eq("Sal"), eq("Espargos"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(service), pageable, 1));
        when(businessRepository.findAllById(Set.of(businessId))).thenReturn(List.of(business));

        // Act
        PageResponse<MarketplaceServiceResponse> response = useCase.execute(
                " Massagem ", null, " Sal ", " Espargos ", pageable
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.totalElements()).isEqualTo(1);
        MarketplaceServiceResponse item = response.content().get(0);
        assertThat(item.name()).isEqualTo("Massagem Relaxante");
        assertThat(item.categoryId()).isEqualTo(categoryId);
        assertThat(item.categorySlug()).isEqualTo("spa_massage");
        assertThat(item.businessName()).isEqualTo("Salão Visual");
        assertThat(item.businessIsland()).isEqualTo("Sal");
        assertThat(item.businessCity()).isEqualTo("Espargos");
        assertThat(item.price()).isEqualByComparingTo(new BigDecimal("3500.00"));

        verify(serviceRepository).searchMarketplaceServices("Massagem", "Sal", "Espargos", pageable);
    }

    @Test
    @DisplayName("Deve pesquisar serviços filtrando por categoria")
    void shouldSearchServicesFilteredByCategory() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("spa_massage").name("Spa & Massagem").build();
        category.setId(categoryId);

        BusinessEntity business = BusinessEntity.builder().name("Spa Sal").island("Sal").city("Santa Maria").build();
        business.setId(businessId);

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .category(category)
                .name("Massagem Pedras Quentes")
                .durationMinutes(60)
                .price(new BigDecimal("4000.00"))
                .isActive(true)
                .build();
        service.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        when(categoryLookupService.findActiveBySlugOrId("spa_massage")).thenReturn(Optional.of(category));
        when(serviceRepository.searchMarketplaceServicesByCategory(null, null, null, categoryId, pageable))
                .thenReturn(new PageImpl<>(List.of(service), pageable, 1));
        when(businessRepository.findAllById(Set.of(businessId))).thenReturn(List.of(business));

        // Act
        PageResponse<MarketplaceServiceResponse> response = useCase.execute(null, "spa_massage", null, null, pageable);

        // Assert
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).categorySlug()).isEqualTo("spa_massage");
    }
}
