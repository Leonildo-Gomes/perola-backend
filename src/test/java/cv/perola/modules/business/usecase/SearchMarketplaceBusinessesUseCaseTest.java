package cv.perola.modules.business.usecase;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
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

    @Spy
    private BusinessMapper businessMapper = new BusinessMapper();

    @InjectMocks
    private SearchMarketplaceBusinessesUseCase useCase;

    @Test
    @DisplayName("Deve pesquisar salões e retornar página com contagem de serviços")
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
        when(businessRepository.searchMarketplaceBusinesses(eq("Beleza"), eq("Santiago"), eq("Praia"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(b1), pageable, 1));
        when(serviceRepository.countByBusinessIdAndIsActiveTrue(b1Id)).thenReturn(5L);

        // Act
        PageResponse<MarketplaceBusinessSummaryResponse> response = useCase.execute(
                " Beleza ", " Santiago ", " Praia ", pageable
        );

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
}
