package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.validator.business.BusinessCategoryValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBusinessUseCaseTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessCategoryRepository businessCategoryRepository;

    @Mock
    private BusinessCategoryValidator businessCategoryValidator;

    @Spy
    private BusinessMapper businessMapper = new BusinessMapper();

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper();

    @InjectMocks
    private UpdateBusinessUseCase updateBusinessUseCase;

    @Test
    @DisplayName("Deve atualizar os campos fornecidos do salão com sucesso")
    void shouldUpdateProvidedFieldsSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        BusinessEntity existing = BusinessEntity.builder()
                .ownerId(ownerId)
                .name("Nome Antigo")
                .island("Santiago")
                .city("Praia")
                .location("Local Antigo")
                .contactPhone("+2389911111")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        existing.setId(businessId);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        UpdateBusinessRequest request = new UpdateBusinessRequest(
                "Nome Novo",
                "São Vicente",
                "Mindelo",
                "Local Novo",
                "+2389922222",
                "UTC",
                null
        );

        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.of(existing));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessCategoryRepository.findAllByBusinessId(businessId)).thenReturn(List.of());

        // Act
        BusinessResponse response = updateBusinessUseCase.execute(businessId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Nome Novo");
        assertThat(response.island()).isEqualTo("São Vicente");
        assertThat(response.city()).isEqualTo("Mindelo");
        assertThat(response.location()).isEqualTo("Local Novo");
        assertThat(response.contactPhone()).isEqualTo("+2389922222");
        assertThat(response.timezone()).isEqualTo("UTC");
        verify(businessRepository).save(existing);
    }

    @Test
    @DisplayName("Deve substituir categorias do salão quando fornecidas no PATCH")
    void shouldReplaceBusinessCategoriesWhenProvided() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        BusinessEntity existing = BusinessEntity.builder()
                .ownerId(UUID.randomUUID())
                .name("Salão Misto")
                .island("Santiago")
                .city("Praia")
                .location("Platô")
                .contactPhone("+2389911111")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        existing.setId(businessId);

        CategoryEntity category = CategoryEntity.builder()
                .slug("spa_massage")
                .name("Spa & Massagem")
                .icon("Flower2")
                .displayOrder(4)
                .build();
        category.setId(catId);

        List<BusinessCategoryItemRequest> catRequests = List.of(new BusinessCategoryItemRequest(catId, true));
        UpdateBusinessRequest request = new UpdateBusinessRequest(
                null, null, null, null, null, null, catRequests
        );

        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.of(existing));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(businessCategoryValidator.validateAndResolveCategories(catRequests)).thenReturn(Map.of(catId, category));
        when(businessCategoryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BusinessResponse response = updateBusinessUseCase.execute(businessId, request);

        // Assert
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).slug()).isEqualTo("spa_massage");
        assertThat(response.categories().get(0).isPrimary()).isTrue();
        verify(businessCategoryRepository).deleteAllByBusinessId(businessId);
        verify(businessCategoryRepository).flush();
    }

    @Test
    @DisplayName("Deve manter os valores originais quando os campos da requisição forem nulos ou vazios")
    void shouldPreserveOriginalValuesWhenRequestFieldsAreNullOrBlank() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        BusinessEntity existing = BusinessEntity.builder()
                .ownerId(UUID.randomUUID())
                .name("Salão Original")
                .island("Santiago")
                .city("Praia")
                .location("Platô")
                .contactPhone("+2389911111")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        existing.setId(businessId);

        UpdateBusinessRequest request = new UpdateBusinessRequest(
                null,
                "   ",
                null,
                "",
                null,
                null,
                null
        );

        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.of(existing));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessCategoryRepository.findAllByBusinessId(businessId)).thenReturn(List.of());

        // Act
        BusinessResponse response = updateBusinessUseCase.execute(businessId, request);

        // Assert
        assertThat(response.name()).isEqualTo("Salão Original");
        assertThat(response.island()).isEqualTo("Santiago");
        assertThat(response.city()).isEqualTo("Praia");
        assertThat(response.location()).isEqualTo("Platô");
        assertThat(response.contactPhone()).isEqualTo("+2389911111");
        assertThat(response.timezone()).isEqualTo("Atlantic/Cape_Verde");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o salão não for encontrado")
    void shouldThrowResourceNotFoundExceptionWhenNotFound() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UpdateBusinessRequest request = new UpdateBusinessRequest(
                "Novo Nome", null, null, null, null, null, null
        );
        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateBusinessUseCase.execute(businessId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Estabelecimento não encontrado ou inativo");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o businessId for nulo")
    void shouldThrowResourceNotFoundExceptionWhenBusinessIdIsNull() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> updateBusinessUseCase.execute(null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Identificador de estabelecimento inválido");
    }
}
