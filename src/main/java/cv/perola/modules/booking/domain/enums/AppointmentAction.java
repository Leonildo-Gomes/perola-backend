package cv.perola.modules.booking.domain.enums;

/**
 * Ações registadas na trilha de auditoria append-only de agendamentos (mapeado para o enum PostgreSQL appointment_action).
 */
public enum AppointmentAction {
    CREATED,
    CONFIRMED,
    RESCHEDULE_REQUESTED,
    RESCHEDULE_CONFIRMED,
    RESCHEDULE_REJECTED,
    CANCEL_REQUESTED,
    CANCELLED,
    COMPLETED
}

