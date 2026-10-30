package cv.perola.core.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio imutável disparado a cada transição de estado de um agendamento.
 */
public record AppointmentLifecycleEvent(
        UUID appointmentId,
        UUID businessId,
        UUID customerId,
        UUID employeeId,
        UUID performedById,
        String action,
        String fromStatus,
        String toStatus,
        Instant startTime,
        Instant endTime,
        String notes,
        Instant occurredAt
) implements BookingDomainEvent {
}

