package cv.perola.modules.notifications.usecase;

import cv.perola.core.shared.event.BookingDomainEvent;
import cv.perola.modules.notifications.mapper.NotificationMapper;
import cv.perola.modules.notifications.mapper.NotificationMapper.BookingNotificationPayload;
import cv.perola.modules.notifications.service.NotificationService;
import cv.perola.modules.notifications.validator.notification.SendNotificationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para envio de notificações de ciclo de vida de agendamentos.
 */
@Service
@RequiredArgsConstructor
public class SendBookingNotificationUseCase {

    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final SendNotificationValidator notificationValidator;

    public BookingNotificationPayload execute(BookingDomainEvent event) {
        notificationValidator.validate(event);

        BookingNotificationPayload payload = notificationMapper.toResponse(event);
        notificationService.dispatch(payload);
        return payload;
    }
}

