package cv.perola.modules.notifications.listeners;

import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.core.shared.event.BookingEventAction;
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
    @DisplayName("Deve delegar evento recebido pelo listener para SendBookingNotificationUseCase")
    void shouldDelegateEventToSendBookingNotificationUseCase() {
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
                .occurredAt(now)
                .build();

        // Act
        bookingNotificationListener.onBookingDomainEvent(event);

        // Assert
        verify(sendBookingNotificationUseCase).execute(event);
    }
}
