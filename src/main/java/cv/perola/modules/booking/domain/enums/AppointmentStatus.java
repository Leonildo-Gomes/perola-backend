package cv.perola.modules.booking.domain.enums;

/**
 * Estados do ciclo de vida de um agendamento (mapeado para o enum PostgreSQL appointment_status).
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    RESCHEDULE_PENDING,
    CANCELLED,
    COMPLETED
}

