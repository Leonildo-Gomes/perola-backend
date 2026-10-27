package cv.perola.modules.business.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.mapper.BusinessMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBusinessUseCaseTest {

    @Mock
    private BusinessRepository businessRepository;

    @Spy
    private BusinessMapper businessMapper = new BusinessMapper();

    @InjectMocks
    private GetBusinessUseCase getBusinessUseCase;

    @Test
    @DisplayName("Deve retornar os dados do salão com sucesso quando existir e estiver ativo")
    void shouldReturnBusinessWhenExistsAndIsActive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        BusinessEntity business = BusinessEntity.builder()
                .ownerId(ownerId)
                .name("Salão Modelo")
                .island("Santiago")
                .city("Praia")
                .location("Fazenda")
                .contactPhone("+2389911223")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        business.setId(businessId);
        business.setCreatedAt(Instant.now());
        business.setUpdatedAt(Instant.now());

        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.of(business));

        // Act
        BusinessResponse response = getBusinessUseCase.execute(businessId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(businessId);
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.name()).isEqualTo("Salão Modelo");
        verify(businessRepository).findByIdAndIsActiveTrue(businessId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o salão não for encontrado ou estiver inativo")
    void shouldThrowResourceNotFoundExceptionWhenBusinessNotFoundOrInactive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getBusinessUseCase.execute(businessId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Estabelecimento não encontrado ou inativo");

        verify(businessRepository).findByIdAndIsActiveTrue(businessId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o businessId for nulo")
    void shouldThrowResourceNotFoundExceptionWhenBusinessIdIsNull() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> getBusinessUseCase.execute(null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Identificador de estabelecimento inválido");
    }
}
