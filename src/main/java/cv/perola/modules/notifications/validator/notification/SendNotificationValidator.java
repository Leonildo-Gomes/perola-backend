package cv.perola.modules.notifications.validator.notification;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.shared.event.BookingDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class SendNotificationValidator {

    public void validate(BookingDomainEvent event) {
        if (event == null) {
            throw new ValidationException("O evento de domínio para envio de notificação não pode ser nulo.");
        }
        if (event.appointmentId() == null) {
            throw new ValidationException("O identificador do agendamento é obrigatório para envio de notificação.");
        }
        if (event.action() == null || event.action().isBlank()) {
            throw new ValidationException("A ação do evento é obrigatória para envio de notificação.");
        }
    }
}
