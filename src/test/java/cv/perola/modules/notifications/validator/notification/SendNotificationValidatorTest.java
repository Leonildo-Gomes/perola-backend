package cv.perola.modules.notifications.validator.notification;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.core.shared.event.BookingEventAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SendNotificationValidatorTest {

    private final SendNotificationValidator validator = new SendNotificationValidator();

    @Test
    @DisplayName("Deve validar evento de domínio válido com sucesso")
    void shouldValidateSuccessfully() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(UUID.randomUUID())
                .businessId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .performedById(UUID.randomUUID())
                .action(BookingEventAction.CREATED)
                .toStatus("PENDING")
                .startTime(now)
                .endTime(now.plusSeconds(3600))
                .notes("Nota")
                .occurredAt(now)
                .customerEmail("cliente@perola.cv")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validator.validate(event));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o evento for nulo")
    void shouldThrowWhenEventIsNull() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando appointmentId for nulo")
    void shouldThrowWhenAppointmentIdIsNull() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(null)
                .action(BookingEventAction.CREATED)
                .startTime(now)
                .endTime(now.plusSeconds(3600))
                .build();

        // Act & Assert
        assertThrows(ValidationException.class, () -> validator.validate(event));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando action for nulo")
    void shouldThrowWhenActionIsNull() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(UUID.randomUUID())
                .action(null)
                .startTime(now)
                .endTime(now.plusSeconds(3600))
                .build();

        // Act & Assert
        assertThrows(ValidationException.class, () -> validator.validate(event));
    }
}
