package cv.perola.modules.notifications.mapper;

import cv.perola.core.shared.event.BookingDomainEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapper dedicado do módulo Notifications para conversão de eventos de domínio em payloads de alerta.
 */
@Component
public class NotificationMapper {

    public record BookingNotificationPayload(
            UUID appointmentId,
            UUID businessId,
            UUID customerId,
            UUID employeeId,
            String action,
            String toStatus,
            String summaryMessage,
            Instant occurredAt
    ) {
    }

    public BookingNotificationPayload toResponse(BookingDomainEvent event) {
        String summary = String.format(
                "Agendamento %s transitou para %s (ação: %s)",
                event.appointmentId(),
                event.toStatus(),
                event.action()
        );
        return new BookingNotificationPayload(
                event.appointmentId(),
                event.businessId(),
                event.customerId(),
                event.employeeId(),
                event.action(),
                event.toStatus(),
                summary,
                event.occurredAt()
        );
    }
}

