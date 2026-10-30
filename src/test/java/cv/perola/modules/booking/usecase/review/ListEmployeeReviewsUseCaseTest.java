package cv.perola.modules.booking.usecase.review;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.mapper.ReviewMapper;
import cv.perola.modules.booking.repository.ReviewRepository;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeeReviewsUseCaseTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private BusinessEmployeeQueryService businessEmployeeQueryService;

    @InjectMocks
    private ListEmployeeReviewsUseCase listEmployeeReviewsUseCase;

    @Test
    @DisplayName("Deve listar avaliações paginadas de um profissional existente")
    void shouldListPaginatedReviewsForEmployee() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Ana", true, Instant.now(), Instant.now()
        );
        ReviewEntity entity = ReviewEntity.builder()
                .appointmentId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .employeeId(employeeId)
                .rating(5)
                .comment("Perfeito!")
                .build();
        ReviewResponse item = new ReviewResponse(
                UUID.randomUUID(),
                entity.getAppointmentId(),
                entity.getCustomerId(),
                employeeId,
                5,
                "Perfeito!",
                OffsetDateTime.now(ZoneOffset.ofHours(-1))
        );

        when(businessEmployeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.empty());
        when(reviewRepository.findAllByEmployeeIdOrderByCreatedAtDesc(eq(employeeId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(reviewMapper.toResponse(eq(entity), any(ZoneId.class))).thenReturn(item);

        // Act
        PageResponse<ReviewResponse> result = listEmployeeReviewsUseCase.execute(employeeId, 0, 20);

        // Assert
        assertThat(result.content()).containsExactly(item);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o profissional não existir")
    void shouldThrowResourceNotFoundExceptionWhenEmployeeNotFound() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        when(businessEmployeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listEmployeeReviewsUseCase.execute(employeeId, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profissional não encontrado.");
    }
}

