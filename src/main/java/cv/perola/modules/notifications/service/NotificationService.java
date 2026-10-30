package cv.perola.modules.notifications.service;

import cv.perola.modules.notifications.mapper.NotificationMapper.BookingNotificationPayload;

/**
 * Serviço de disparo de alertas e notificações (logs em memória no MVP conforme ADR-06).
 */
public interface NotificationService {

    void dispatch(BookingNotificationPayload payload);
}

