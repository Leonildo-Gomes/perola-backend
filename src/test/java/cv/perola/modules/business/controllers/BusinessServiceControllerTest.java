package cv.perola.modules.business.controllers;

import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.usecase.CreateServiceUseCase;
import cv.perola.modules.business.usecase.DeactivateServiceUseCase;
import cv.perola.modules.business.usecase.ListServicesUseCase;
import cv.perola.modules.business.usecase.UpdateServiceUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceControllerTest {

    @Mock
    private CreateServiceUseCase createServiceUseCase;

    @Mock
    private ListServicesUseCase listServicesUseCase;

    @Mock
    private UpdateServiceUseCase updateServiceUseCase;

    @Mock
    private DeactivateServiceUseCase deactivateServiceUseCase;

    @InjectMocks
    private BusinessServiceController controller;

    @Test
    @DisplayName("Deve chamar CreateServiceUseCase e retornar HTTP 201 Created")
    void shouldCreateServiceAndReturn201() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest("Corte", "Desc", 30, new BigDecimal("1000.00"));
        ServiceResponse expected = new ServiceResponse(
                UUID.randomUUID(), businessId, "Corte", "Desc", 30, new BigDecimal("1000.00"), true, Instant.now(), Instant.now()
        );
        when(createServiceUseCase.execute(businessId, request)).thenReturn(expected);

        // Act
        ResponseEntity<ServiceResponse> response = controller.createService(businessId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(createServiceUseCase).execute(businessId, request);
    }

    @Test
    @DisplayName("Deve chamar ListServicesUseCase e retornar HTTP 200 OK com lista de serviços")
    void shouldListServicesAndReturn200() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        ServiceResponse s1 = new ServiceResponse(
                UUID.randomUUID(), businessId, "Corte", "Desc", 30, new BigDecimal("1000.00"), true, Instant.now(), Instant.now()
        );
        when(listServicesUseCase.execute(businessId)).thenReturn(List.of(s1));

        // Act
        ResponseEntity<List<ServiceResponse>> response = controller.listServices(businessId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(listServicesUseCase).execute(businessId);
    }

    @Test
    @DisplayName("Deve chamar UpdateServiceUseCase e retornar HTTP 200 OK")
    void shouldUpdateServiceAndReturn200() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UpdateServiceRequest request = new UpdateServiceRequest("Corte Novo", "Desc Nova", 40, new BigDecimal("1200.00"), true);
        ServiceResponse expected = new ServiceResponse(
                serviceId, businessId, "Corte Novo", "Desc Nova", 40, new BigDecimal("1200.00"), true, Instant.now(), Instant.now()
        );
        when(updateServiceUseCase.execute(businessId, serviceId, request)).thenReturn(expected);

        // Act
        ResponseEntity<ServiceResponse> response = controller.updateService(businessId, serviceId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(updateServiceUseCase).execute(businessId, serviceId, request);
    }

    @Test
    @DisplayName("Deve chamar DeactivateServiceUseCase e retornar HTTP 204 No Content")
    void shouldDeactivateServiceAndReturn204() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        // Act
        ResponseEntity<Void> response = controller.deactivateService(businessId, serviceId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(deactivateServiceUseCase).execute(businessId, serviceId);
    }
}
