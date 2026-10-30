package cv.perola.modules.notifications.listeners;

import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.modules.notifications.usecase.notification.SendBookingNotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    @Mock
    private SendBookingNotificationUseCase sendBookingNotificationUseCase;

    @InjectMocks
    private BookingNotificationListener bookingNotificationListener;

    @Test
    @DisplayName("Deve delegar evento recebido pelo @EventListener para SendBookingNotificationUseCase")
    void shouldDelegateEventToSendBookingNotificationUseCase() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = new AppointmentLifecycleEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "COMPLETED", "CONFIRMED", "COMPLETED", now, now.plusSeconds(3600), null, now
        );

        // Act
        bookingNotificationListener.onBookingDomainEvent(event);

        // Assert
        verify(sendBookingNotificationUseCase).execute(event);
    }
}

