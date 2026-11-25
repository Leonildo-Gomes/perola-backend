package cv.perola.modules.notifications.listeners;

import cv.perola.core.shared.event.BookingDomainEvent;
import cv.perola.modules.notifications.usecase.notification.SendBookingNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener em memória desacoplado que reage a eventos de domínio do módulo
 * Booking (ADR-06).
 */
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final SendBookingNotificationUseCase sendBookingNotificationUseCase;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingDomainEvent(BookingDomainEvent event) {
        sendBookingNotificationUseCase.execute(event);
    }
}
