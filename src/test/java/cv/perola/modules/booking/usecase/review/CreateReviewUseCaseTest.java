package cv.perola.modules.booking.usecase.review;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.mapper.ReviewMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.ReviewRepository;
import cv.perola.modules.booking.validator.review.CreateReviewValidator;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReviewUseCaseTest {

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private ReviewRepository reviewRepository;

        @Mock
        private ReviewMapper reviewMapper;

        @Mock
        private BusinessEmployeeQueryService businessEmployeeQueryService;

        private CreateReviewValidator createReviewValidator;
        private CreateReviewUseCase createReviewUseCase;

        @BeforeEach
        void setUp() {
                createReviewValidator = new CreateReviewValidator(appointmentRepository, reviewRepository);
                createReviewUseCase = new CreateReviewUseCase(
                                reviewRepository,
                                reviewMapper,
                                businessEmployeeQueryService,
                                createReviewValidator);
        }

        @Test
        @DisplayName("Deve criar avaliação com sucesso quando o agendamento estiver COMPLETED e o autor for o cliente")
        void shouldCreateReviewWhenAppointmentIsCompletedAndUserIsCustomer() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                CreateReviewRequest request = new CreateReviewRequest(5, "Excelente profissional!");

                AppointmentEntity appointment = AppointmentEntity.builder()
                                .businessId(businessId)
                                .customerId(customerId)
                                .employeeId(employeeId)
                                .status(AppointmentStatus.COMPLETED)
                                .build();
                ReflectionTestUtils.setField(appointment, "id", appointmentId);

                ReviewEntity entity = ReviewEntity.builder()
                                .appointmentId(appointmentId)
                                .customerId(customerId)
                                .employeeId(employeeId)
                                .rating(5)
                                .comment("Excelente profissional!")
                                .build();

                ReviewResponse expectedResponse = new ReviewResponse(
                                UUID.randomUUID(),
                                appointmentId,
                                customerId,
                                employeeId,
                                5,
                                "Excelente profissional!",
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)));

                BusinessResponse businessResponse = new BusinessResponse(
                                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                                "Platô", "+2389911223", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now());

                when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
                when(reviewRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
                when(reviewMapper.toEntity(appointment, request)).thenReturn(entity);
                when(reviewRepository.saveAndFlush(entity)).thenReturn(entity);
                when(businessEmployeeQueryService.findBusinessById(businessId))
                                .thenReturn(Optional.of(businessResponse));
                when(reviewMapper.toResponse(eq(entity), any(ZoneId.class))).thenReturn(expectedResponse);

                // Act
                ReviewResponse result = createReviewUseCase.execute(appointmentId, customerId, request);

                // Assert
                assertThat(result).isEqualTo(expectedResponse);
                verify(reviewRepository).saveAndFlush(entity);
        }

        @ParameterizedTest
        @EnumSource(value = AppointmentStatus.class, names = { "PENDING", "CONFIRMED", "RESCHEDULE_PENDING",
                        "CANCELLED" })
        @DisplayName("Deve lançar ValidationException (HTTP 400) quando agendamento não estiver COMPLETED")
        void shouldThrowValidationExceptionWhenAppointmentIsNotCompleted(AppointmentStatus nonCompletedStatus) {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                CreateReviewRequest request = new CreateReviewRequest(4, "Comentário");

                AppointmentEntity appointment = AppointmentEntity.builder()
                                .customerId(customerId)
                                .status(nonCompletedStatus)
                                .build();
                when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

                // Act & Assert
                assertThatThrownBy(() -> createReviewUseCase.execute(appointmentId, customerId, request))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("Deve lançar ConflictException (HTTP 409) quando já existir avaliação para o agendamento")
        void shouldThrowConflictExceptionWhenReviewAlreadyExistsForAppointment() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                CreateReviewRequest request = new CreateReviewRequest(5, "Duplicada");

                AppointmentEntity appointment = AppointmentEntity.builder()
                                .customerId(customerId)
                                .status(AppointmentStatus.COMPLETED)
                                .build();
                when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
                when(reviewRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> createReviewUseCase.execute(appointmentId, customerId, request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessage("Já existe uma avaliação registada para este agendamento.");
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException (HTTP 403) quando outro utilizador tentar avaliar o agendamento")
        void shouldThrowAccessDeniedExceptionWhenUserIsNotAppointmentCustomer() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                UUID otherUserId = UUID.randomUUID();
                CreateReviewRequest request = new CreateReviewRequest(5, "Tentativa de outro cliente");

                AppointmentEntity appointment = AppointmentEntity.builder()
                                .customerId(customerId)
                                .status(AppointmentStatus.COMPLETED)
                                .build();
                when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

                // Act & Assert
                assertThatThrownBy(() -> createReviewUseCase.execute(appointmentId, otherUserId, request))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessage("Apenas o cliente autor do agendamento pode submeter uma avaliação.");
        }

        @ParameterizedTest
        @ValueSource(ints = { 0, -1, 6, 10 })
        @DisplayName("Deve lançar ValidationException quando rating estiver fora do intervalo 1..5")
        void shouldThrowValidationExceptionWhenRatingIsOutOfRange(int invalidRating) {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                CreateReviewRequest request = new CreateReviewRequest(invalidRating, "Nota inválida");

                // Act & Assert
                assertThatThrownBy(() -> createReviewUseCase.execute(appointmentId, customerId, request))
                                .isInstanceOf(ValidationException.class)
                                .hasMessage("A nota de avaliação (rating) deve estar compreendida entre 1 e 5.");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o agendamento não existir")
        void shouldThrowResourceNotFoundExceptionWhenAppointmentDoesNotExist() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                CreateReviewRequest request = new CreateReviewRequest(5, "Comentário");
                when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> createReviewUseCase.execute(appointmentId, customerId, request))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessage("Agendamento não encontrado.");
        }
}
