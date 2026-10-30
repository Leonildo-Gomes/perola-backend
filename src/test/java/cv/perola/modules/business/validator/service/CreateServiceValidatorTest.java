package cv.perola.modules.business.validator.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.repository.BusinessRepository;
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
class CreateServiceValidatorTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private CreateServiceValidator validator;

    @Test
    @DisplayName("Deve validar com sucesso quando salão existe e não há serviço com mesmo nome ativo")
    void shouldValidateSuccessfully() {
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest("Corte de Cabelo", "Descrição", 45, BigDecimal.valueOf(1500));

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte de Cabelo", businessId)).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(businessId, request));

        verify(businessRepository).existsById(businessId);
        verify(serviceRepository).existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte de Cabelo", businessId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o salão não existir")
    void shouldThrowWhenBusinessNotFound() {
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest("Corte", "Desc", 30, BigDecimal.valueOf(1000));

        when(businessRepository.existsById(businessId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> validator.validate(businessId, request));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando já existir serviço ativo com o mesmo nome")
    void shouldThrowWhenServiceAlreadyExists() {
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest("Corte", "Desc", 30, BigDecimal.valueOf(1000));

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte", businessId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(businessId, request));
    }
}

