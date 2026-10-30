package cv.perola.modules.booking.validator.review;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReviewValidatorTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private CreateReviewValidator validator;

    @Test
    @DisplayName("Deve validar avaliação com sucesso quando o agendamento estiver COMPLETED e sem avaliação prévia")
    void shouldValidateSuccessfully() {
        UUID customerId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .customerId(customerId)
                .status(AppointmentStatus.COMPLETED)
                .build();
        appointment.setId(appointmentId);

        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente!");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByAppointmentId(appointmentId)).thenReturn(false);

        AppointmentEntity result = assertDoesNotThrow(() -> validator.validate(appointmentId, customerId, request));

        assertNotNull(result);
        verify(reviewRepository).existsByAppointmentId(appointmentId);
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o rating for inválido")
    void shouldThrowWhenRatingIsInvalid() {
        CreateReviewRequest request = new CreateReviewRequest(6, "Excelente!");

        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), UUID.randomUUID(), request));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o agendamento não for encontrado")
    void shouldThrowWhenAppointmentNotFound() {
        UUID appointmentId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente!");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> validator.validate(appointmentId, UUID.randomUUID(), request));
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando outro cliente tentar avaliar")
    void shouldThrowWhenCustomerIsNotAuthor() {
        UUID customerId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .customerId(customerId)
                .status(AppointmentStatus.COMPLETED)
                .build();

        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente!");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(AccessDeniedException.class, () -> validator.validate(appointmentId, UUID.randomUUID(), request));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o agendamento não estiver COMPLETED")
    void shouldThrowWhenAppointmentIsNotCompleted() {
        UUID customerId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .customerId(customerId)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente!");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThrows(ValidationException.class, () -> validator.validate(appointmentId, customerId, request));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando já existir avaliação para o agendamento")
    void shouldThrowWhenReviewAlreadyExists() {
        UUID customerId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .customerId(customerId)
                .status(AppointmentStatus.COMPLETED)
                .build();
        appointment.setId(appointmentId);

        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente!");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(appointmentId, customerId, request));
    }
}
