package cv.perola.core.shared.event;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

/**
 * Evento de domínio imutável disparado a cada transição de estado de um
 * agendamento.
 */
@Builder
public record AppointmentLifecycleEvent(
        UUID appointmentId,
        UUID businessId,
        UUID customerId,
        UUID employeeId,
        UUID performedById,
        BookingEventAction action,
        String fromStatus,
        String toStatus,
        Instant startTime,
        Instant endTime,
        String notes,
        Instant occurredAt,
        String customerEmail,
        String customerName,
        String businessName,
        String employeeName,
        String serviceName,
        String servicePriceCve,
        String businessTimeZone) implements BookingDomainEvent {
}
