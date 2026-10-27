package cv.perola.modules.business.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.usecase.CreateBusinessUseCase;
import cv.perola.modules.business.usecase.GetBusinessUseCase;
import cv.perola.modules.business.usecase.UpdateBusinessUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessControllerTest {

    @Mock
    private CreateBusinessUseCase createBusinessUseCase;

    @Mock
    private GetBusinessUseCase getBusinessUseCase;

    @Mock
    private UpdateBusinessUseCase updateBusinessUseCase;

    @InjectMocks
    private BusinessController businessController;

    @Test
    @DisplayName("Deve chamar CreateBusinessUseCase e retornar HTTP 201 Created com BusinessResponse")
    void shouldCreateBusinessAndReturn201Created() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId,
                "proprietario@example.com",
                "hash",
                List.of("CUSTOMER"),
                Collections.emptyList(),
                true
        );
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Pérola",
                "Santiago",
                "Praia",
                "Platô",
                "+2382611122",
                "Atlantic/Cape_Verde"
        );
        BusinessResponse expectedResponse = new BusinessResponse(
                businessId,
                userId,
                "Salão Pérola",
                "Santiago",
                "Praia",
                "Platô",
                "+2382611122",
                "Atlantic/Cape_Verde",
                true,
                Instant.now(),
                Instant.now()
        );

        when(createBusinessUseCase.execute(userId, request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<BusinessResponse> response = businessController.createBusiness(principal, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(createBusinessUseCase).execute(userId, request);
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException ao criar negócio sem principal autenticado")
    void shouldThrowAuthenticationFailedExceptionWhenPrincipalIsNull() {
        // Arrange
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão", "Santiago", "Praia", "Rua 1", "+2382611122", null
        );

        // Act & Assert
        assertThatThrownBy(() -> businessController.createBusiness(null, request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Autenticação necessária");
    }

    @Test
    @DisplayName("Deve chamar GetBusinessUseCase e retornar HTTP 200 OK com BusinessResponse")
    void shouldGetBusinessAndReturn200Ok() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        BusinessResponse expectedResponse = new BusinessResponse(
                businessId,
                UUID.randomUUID(),
                "Salão Pérola",
                "Santiago",
                "Praia",
                "Platô",
                "+2382611122",
                "Atlantic/Cape_Verde",
                true,
                Instant.now(),
                Instant.now()
        );

        when(getBusinessUseCase.execute(businessId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<BusinessResponse> response = businessController.getBusiness(businessId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(getBusinessUseCase).execute(businessId);
    }

    @Test
    @DisplayName("Deve chamar UpdateBusinessUseCase e retornar HTTP 200 OK com BusinessResponse")
    void shouldUpdateBusinessAndReturn200Ok() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UpdateBusinessRequest request = new UpdateBusinessRequest(
                "Salão VIP", null, null, null, null, null
        );
        BusinessResponse expectedResponse = new BusinessResponse(
                businessId,
                UUID.randomUUID(),
                "Salão VIP",
                "Santiago",
                "Praia",
                "Platô",
                "+2382611122",
                "Atlantic/Cape_Verde",
                true,
                Instant.now(),
                Instant.now()
        );

        when(updateBusinessUseCase.execute(businessId, request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<BusinessResponse> response = businessController.updateBusiness(businessId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(updateBusinessUseCase).execute(businessId, request);
    }
}
