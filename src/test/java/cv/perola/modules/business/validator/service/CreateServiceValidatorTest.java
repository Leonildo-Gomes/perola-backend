package cv.perola.modules.business.validator.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.CategoryRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateServiceValidatorTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private CreateServiceValidator validator;

    @Test
    @DisplayName("Deve validar com sucesso quando salão e categoria existem e não há serviço com mesmo nome ativo")
    void shouldValidateSuccessfully() {
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("hair").name("Cabeleireiro").isActive(true).build();
        category.setId(categoryId);
        CreateServiceRequest request = new CreateServiceRequest(
                categoryId, "Corte de Cabelo", "Descrição", 45, BigDecimal.valueOf(1500)
        );

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte de Cabelo", businessId)).thenReturn(false);

        CategoryEntity result = validator.validate(businessId, request);

        assertEquals(category, result);
        verify(businessRepository).existsById(businessId);
        verify(categoryRepository).findByIdAndIsActiveTrue(categoryId);
        verify(serviceRepository).existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte de Cabelo", businessId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o salão não existir")
    void shouldThrowWhenBusinessNotFound() {
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest(
                UUID.randomUUID(), "Corte", "Desc", 30, BigDecimal.valueOf(1000)
        );

        when(businessRepository.existsById(businessId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> validator.validate(businessId, request));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando a categoria não existir ou estiver inativa")
    void shouldThrowWhenCategoryNotFound() {
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest(
                categoryId, "Corte", "Desc", 30, BigDecimal.valueOf(1000)
        );

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> validator.validate(businessId, request));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando já existir serviço ativo com o mesmo nome")
    void shouldThrowWhenServiceAlreadyExists() {
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("hair").name("Cabeleireiro").isActive(true).build();
        category.setId(categoryId);
        CreateServiceRequest request = new CreateServiceRequest(
                categoryId, "Corte", "Desc", 30, BigDecimal.valueOf(1000)
        );

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue("Corte", businessId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(businessId, request));
    }
}
