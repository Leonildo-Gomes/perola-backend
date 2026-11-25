package cv.perola.core.shared.event;

/**
 * Tipos de ação publicados nos eventos de domínio de agendamento
 * (linguagem partilhada entre produtores e listeners sem acoplamento
 * inter-modular).
 */
public enum BookingEventAction {
    CREATED,
    CONFIRMED,
    RESCHEDULE_REQUESTED,
    RESCHEDULE_CONFIRMED,
    RESCHEDULE_REJECTED,
    CANCEL_REQUESTED,
    CANCELLED,
    COMPLETED
}
