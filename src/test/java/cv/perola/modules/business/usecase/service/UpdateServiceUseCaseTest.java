package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class UpdateServiceUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private UpdateServiceUseCase updateServiceUseCase;

    @Test
    @DisplayName("Deve atualizar campos do serviço com sucesso")
    void shouldUpdateServiceFieldsSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        ServiceEntity existing = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Simples")
                .description("Corte rápido")
                .durationMinutes(20)
                .price(new BigDecimal("700.00"))
                .isActive(true)
                .build();
        existing.setId(serviceId);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        UpdateServiceRequest request = new UpdateServiceRequest(
                "Corte Especial",
                "Corte detalhado com lavagem",
                40,
                new BigDecimal("1200.00"),
                true
        );

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(existing));
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte Especial", businessId)).thenReturn(false);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ServiceResponse response = updateServiceUseCase.execute(businessId, serviceId, request);

        // Assert
        assertThat(response.name()).isEqualTo("Corte Especial");
        assertThat(response.description()).isEqualTo("Corte detalhado com lavagem");
        assertThat(response.durationMinutes()).isEqualTo(40);
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("1200.00"));

        verify(serviceRepository).save(existing);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando serviço não existir")
    void shouldThrowResourceNotFoundWhenServiceDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UpdateServiceRequest request = new UpdateServiceRequest("Novo Nome", null, null, null, null);

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateServiceUseCase.execute(businessId, serviceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Serviço não encontrado");

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando novo nome colidir com outro serviço ativo")
    void shouldThrowConflictWhenNewNameCollidesWithExistingService() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        ServiceEntity existing = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Antigo")
                .durationMinutes(30)
                .price(new BigDecimal("1000.00"))
                .isActive(true)
                .build();
        existing.setId(serviceId);

        UpdateServiceRequest request = new UpdateServiceRequest("Corte Existente", null, null, null, null);

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(existing));
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte Existente", businessId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> updateServiceUseCase.execute(businessId, serviceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um serviço ativo com o nome 'Corte Existente'");

        verify(serviceRepository, never()).save(any());
    }
}
