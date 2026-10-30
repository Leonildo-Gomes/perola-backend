package cv.perola.modules.booking.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.usecase.review.CreateReviewUseCase;
import cv.perola.modules.booking.usecase.review.ListBusinessReviewsUseCase;
import cv.perola.modules.booking.usecase.review.ListEmployeeReviewsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private CreateReviewUseCase createReviewUseCase;

    @Mock
    private ListBusinessReviewsUseCase listBusinessReviewsUseCase;

    @Mock
    private ListEmployeeReviewsUseCase listEmployeeReviewsUseCase;

    @InjectMocks
    private ReviewController reviewController;

    @Test
    @DisplayName("Deve retornar HTTP 201 Created ao submeter avaliação")
    void shouldReturnCreatedWhenCreatingReview() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "cliente@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true
        );
        CreateReviewRequest request = new CreateReviewRequest(5, "Ótimo serviço!");
        ReviewResponse expected = new ReviewResponse(
                UUID.randomUUID(),
                appointmentId,
                userId,
                UUID.randomUUID(),
                5,
                "Ótimo serviço!",
                OffsetDateTime.now(ZoneOffset.ofHours(-1))
        );
        when(createReviewUseCase.execute(appointmentId, userId, request)).thenReturn(expected);

        // Act
        ResponseEntity<ReviewResponse> response = reviewController.createReview(appointmentId, principal, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(createReviewUseCase).execute(appointmentId, userId, request);
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException ao submeter avaliação sem principal")
    void shouldThrowAuthenticationFailedExceptionWhenPrincipalIsNull() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(5, "Ótimo!");

        // Act & Assert
        assertThatThrownBy(() -> reviewController.createReview(appointmentId, null, request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Autenticação necessária.");
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao listar avaliações do salão")
    void shouldReturnOkWhenListingBusinessReviews() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        ReviewResponse item = new ReviewResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                5,
                "Excelente",
                OffsetDateTime.now(ZoneOffset.ofHours(-1))
        );
        PageResponse<ReviewResponse> expected = new PageResponse<>(List.of(item), 0, 20, 1L, 1);
        when(listBusinessReviewsUseCase.execute(businessId, 0, 20)).thenReturn(expected);

        // Act
        ResponseEntity<PageResponse<ReviewResponse>> response = reviewController.getBusinessReviews(businessId, 0, 20);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(listBusinessReviewsUseCase).execute(businessId, 0, 20);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao listar avaliações públicas do profissional")
    void shouldReturnOkWhenListingEmployeeReviews() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        ReviewResponse item = new ReviewResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                employeeId,
                4,
                "Muito bom",
                OffsetDateTime.now(ZoneOffset.ofHours(-1))
        );
        PageResponse<ReviewResponse> expected = new PageResponse<>(List.of(item), 0, 20, 1L, 1);
        when(listEmployeeReviewsUseCase.execute(employeeId, 0, 20)).thenReturn(expected);

        // Act
        ResponseEntity<PageResponse<ReviewResponse>> response = reviewController.getEmployeeReviews(employeeId, 0, 20);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(listEmployeeReviewsUseCase).execute(employeeId, 0, 20);
    }
}

