package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.CategoryRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.validator.service.CreateServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateServiceUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    private CreateServiceValidator createServiceValidator;
    private CreateServiceUseCase createServiceUseCase;

    @BeforeEach
    void setUp() {
        createServiceValidator = new CreateServiceValidator(businessRepository, categoryRepository, serviceRepository);
        createServiceUseCase = new CreateServiceUseCase(
                serviceRepository,
                serviceMapper,
                createServiceValidator
        );
    }

    @Test
    @DisplayName("Deve criar serviço categorizado com sucesso quando dados forem válidos")
    void shouldCreateServiceSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder()
                .slug("barbershop")
                .name("Barbearia")
                .icon("Scissors")
                .displayOrder(1)
                .isActive(true)
                .build();
        category.setId(categoryId);

        CreateServiceRequest request = new CreateServiceRequest(
                categoryId,
                "Corte Masculino",
                "Corte com máquina e tesoura",
                30,
                new BigDecimal("1000.00")
        );

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte Masculino", businessId))
                .thenReturn(false);

        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> {
            ServiceEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        // Act
        ServiceResponse response = createServiceUseCase.execute(businessId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.businessId()).isEqualTo(businessId);
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categorySlug()).isEqualTo("barbershop");
        assertThat(response.categoryName()).isEqualTo("Barbearia");
        assertThat(response.name()).isEqualTo("Corte Masculino");
        assertThat(response.description()).isEqualTo("Corte com máquina e tesoura");
        assertThat(response.durationMinutes()).isEqualTo(30);
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.isActive()).isTrue();

        verify(serviceRepository).save(any(ServiceEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando salão não existir")
    void shouldThrowResourceNotFoundWhenBusinessDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest(
                UUID.randomUUID(),
                "Corte",
                "Desc",
                30,
                new BigDecimal("1000.00")
        );

        when(businessRepository.existsById(businessId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> createServiceUseCase.execute(businessId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Salão não encontrado");

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando categoria não existir ou estiver inativa")
    void shouldThrowResourceNotFoundWhenCategoryDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest(
                categoryId,
                "Corte",
                "Desc",
                30,
                new BigDecimal("1000.00")
        );

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createServiceUseCase.execute(businessId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Categoria não encontrada ou inativa");

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando já existir serviço ativo com mesmo nome")
    void shouldThrowConflictWhenServiceNameAlreadyExists() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("barbershop").name("Barbearia").build();
        category.setId(categoryId);

        CreateServiceRequest request = new CreateServiceRequest(
                categoryId,
                "Corte Masculino",
                "Desc",
                30,
                new BigDecimal("1000.00")
        );

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte Masculino", businessId))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createServiceUseCase.execute(businessId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um serviço ativo com o nome 'Corte Masculino'");

        verify(serviceRepository, never()).save(any());
    }
}