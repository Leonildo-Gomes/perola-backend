package cv.perola.modules.notifications.validator.notification;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.shared.event.AppointmentLifecycleEvent;
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
        AppointmentLifecycleEvent event = new AppointmentLifecycleEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREATED",
                null,
                "PENDING",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "Nota",
                Instant.now()
        );

        assertDoesNotThrow(() -> validator.validate(event));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o evento for nulo")
    void shouldThrowWhenEventIsNull() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando appointmentId for nulo")
    void shouldThrowWhenAppointmentIdIsNull() {
        AppointmentLifecycleEvent event = new AppointmentLifecycleEvent(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREATED",
                null,
                "PENDING",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "Nota",
                Instant.now()
        );

        assertThrows(ValidationException.class, () -> validator.validate(event));
    }
}

