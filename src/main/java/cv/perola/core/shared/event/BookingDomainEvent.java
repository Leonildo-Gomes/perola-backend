package cv.perola.core.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface selada para eventos de domínio do ciclo de vida de agendamentos.
 * Reside em core.shared para permitir desacoplamento estrito entre os módulos Booking e Notifications.
 */
public sealed interface BookingDomainEvent permits AppointmentLifecycleEvent {

    UUID appointmentId();

    UUID businessId();

    UUID customerId();

    UUID employeeId();

    UUID performedById();

    String action();

    String fromStatus();

    String toStatus();

    Instant startTime();

    Instant endTime();

    String notes();

    Instant occurredAt();
}

