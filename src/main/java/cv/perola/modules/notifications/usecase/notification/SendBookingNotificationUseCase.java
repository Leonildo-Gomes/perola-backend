package cv.perola.modules.notifications.usecase.notification;

import cv.perola.core.shared.event.BookingDomainEvent;
import cv.perola.modules.notifications.mapper.NotificationMapper;
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

    private final SendNotificationValidator notificationValidator;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    public void execute(BookingDomainEvent event) {
        notificationValidator.validate(event);
        notificationMapper.toEmailRequest(event)
                .ifPresent(notificationService::send);
    }
}
