package cv.perola.modules.notifications.listeners;

import cv.perola.core.shared.event.BookingDomainEvent;
import cv.perola.modules.notifications.usecase.SendBookingNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener em memória desacoplado que reage a eventos de domínio do módulo Booking (ADR-06).
 */
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final SendBookingNotificationUseCase sendBookingNotificationUseCase;

    @EventListener
    public void onBookingDomainEvent(BookingDomainEvent event) {
        sendBookingNotificationUseCase.execute(event);
    }
}

