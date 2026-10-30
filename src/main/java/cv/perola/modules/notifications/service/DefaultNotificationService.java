package cv.perola.modules.notifications.service;

import cv.perola.modules.notifications.mapper.NotificationMapper.BookingNotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationService.class);

    @Override
    public void dispatch(BookingNotificationPayload payload) {
        logger.info(
                "Notificação enviada para o utilizador {} e profissional {} do salão {}: {}",
                payload.customerId(),
                payload.employeeId(),
                payload.businessId(),
                payload.summaryMessage()
        );
    }
}

