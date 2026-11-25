package cv.perola.modules.notifications.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {
    APPOINTMENT_CREATED("mail/booking/appointment-created", "Recebemos a sua marcação — %s"),
    APPOINTMENT_CONFIRMED("mail/booking/appointment-confirmed", "Marcação confirmada em %s"),
    APPOINTMENT_CANCELLED("mail/booking/appointment-cancelled", "Marcação cancelada — %s"),
    RESCHEDULE_REQUESTED("mail/booking/reschedule-requested", "Nova proposta de horário — %s"),
    WELCOME_USER("mail/auth/welcome", "Bem-vindo(a) à Pérola, %s!");

    private final String templatePath;
    private final String subjectPattern;

    public String formatSubject(String arg) {
        return String.format(subjectPattern, arg);
    }

}
