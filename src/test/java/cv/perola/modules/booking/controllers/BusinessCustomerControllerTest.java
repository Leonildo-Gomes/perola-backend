package cv.perola.modules.booking.controllers;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.BusinessCustomerDetailResponse;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.dto.UpdateBusinessCustomerRequest;
import cv.perola.modules.booking.usecase.customer.GetBusinessCustomerDetailUseCase;
import cv.perola.modules.booking.usecase.customer.ListBusinessCustomersUseCase;
import cv.perola.modules.booking.usecase.customer.UpdateBusinessCustomerUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessCustomerControllerTest {

    @Mock
    private ListBusinessCustomersUseCase listBusinessCustomersUseCase;

    @Mock
    private GetBusinessCustomerDetailUseCase getBusinessCustomerDetailUseCase;

    @Mock
    private UpdateBusinessCustomerUseCase updateBusinessCustomerUseCase;

    @InjectMocks
    private BusinessCustomerController controller;

    @Test
    @DisplayName("Deve retornar 200 OK com lista paginada de clientes")
    void shouldReturnOkWhenListingCustomers() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BusinessCustomerResponse customer = new BusinessCustomerResponse(
                customerId, "Márcia Gonçalves", "marcia@cv.com", "+2389912345", true,
                CustomerTier.VIP, "Notas", 10, 10, 0, new BigDecimal("25000.00"),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        PageResponse<BusinessCustomerResponse> pageResponse = new PageResponse<>(List.of(customer), 0, 20, 1, 1);

        when(listBusinessCustomersUseCase.execute(businessId, "marcia", CustomerTier.VIP, 0, 20))
                .thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponse<BusinessCustomerResponse>> response = controller.listCustomers(
                businessId, "marcia", CustomerTier.VIP, 0, 20
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pageResponse);
        verify(listBusinessCustomersUseCase).execute(businessId, "marcia", CustomerTier.VIP, 0, 20);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com detalhe da ficha do cliente e marcações recentes")
    void shouldReturnOkWhenGettingCustomerDetail() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BusinessCustomerResponse customer = new BusinessCustomerResponse(
                customerId, "Márcia Gonçalves", "marcia@cv.com", "+2389912345", true,
                CustomerTier.VIP, "Notas", 10, 10, 0, new BigDecimal("25000.00"),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        BusinessCustomerDetailResponse detailResponse = new BusinessCustomerDetailResponse(customer, List.of());

        when(getBusinessCustomerDetailUseCase.execute(businessId, customerId)).thenReturn(detailResponse);

        // Act
        ResponseEntity<BusinessCustomerDetailResponse> response = controller.getCustomerDetail(businessId, customerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detailResponse);
        verify(getBusinessCustomerDetailUseCase).execute(businessId, customerId);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com ficha atualizada após PATCH")
    void shouldReturnOkWhenUpdatingCustomer() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(
                CustomerTier.VIP, "Notas atualizadas."
        );
        BusinessCustomerResponse updated = new BusinessCustomerResponse(
                customerId, "Márcia Gonçalves", "marcia@cv.com", "+2389912345", true,
                CustomerTier.VIP, "Notas atualizadas.", 10, 10, 0, new BigDecimal("25000.00"),
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(updateBusinessCustomerUseCase.execute(businessId, customerId, request)).thenReturn(updated);

        // Act
        ResponseEntity<BusinessCustomerResponse> response = controller.updateCustomer(businessId, customerId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(updateBusinessCustomerUseCase).execute(businessId, customerId, request);
    }
}
