package cv.perola.modules.business.validator.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceStatusValidatorTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceStatusValidator validator;

    @Test
    @DisplayName("Deve validar reativação com sucesso quando não há outro serviço ativo com o mesmo nome")
    void shouldValidateActivationSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Manicure Simples")
                .description("Cuidado de unhas")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(1000))
                .isActive(false)
                .build();

        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Manicure Simples", businessId))
                .thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateActivation(businessId, service));

        verify(serviceRepository).existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Manicure Simples", businessId);
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando já existir outro serviço ativo com o mesmo nome")
    void shouldThrowConflictExceptionWhenActiveServiceWithSameNameExists() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Manicure Simples")
                .description("Cuidado de unhas")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(1000))
                .isActive(false)
                .build();

        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Manicure Simples", businessId))
                .thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> validator.validateActivation(businessId, service));

        verify(serviceRepository).existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Manicure Simples", businessId);
    }
}

