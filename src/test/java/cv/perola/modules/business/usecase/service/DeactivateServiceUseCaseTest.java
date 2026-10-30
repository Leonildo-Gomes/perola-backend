package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateServiceUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private DeactivateServiceUseCase deactivateServiceUseCase;

    @Test
    @DisplayName("Deve desativar serviço com soft delete com sucesso")
    void shouldDeactivateServiceSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Serviço a Desativar")
                .durationMinutes(30)
                .price(new BigDecimal("1000.00"))
                .isActive(true)
                .build();
        service.setId(serviceId);

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));

        // Act
        deactivateServiceUseCase.execute(businessId, serviceId);

        // Assert
        assertThat(service.isActive()).isFalse();
        verify(serviceRepository).save(service);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando serviço não for encontrado para desativação")
    void shouldThrowResourceNotFoundWhenServiceNotFound() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deactivateServiceUseCase.execute(businessId, serviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Serviço não encontrado");

        verify(serviceRepository, never()).save(any());
    }
}
