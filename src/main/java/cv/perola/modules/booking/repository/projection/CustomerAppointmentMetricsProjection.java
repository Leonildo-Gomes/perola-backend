package cv.perola.modules.booking.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projeção agregada de métricas de agendamento de um cliente num determinado salão.
 */
public interface CustomerAppointmentMetricsProjection {

    UUID getCustomerId();

    long getTotalAppointments();

    long getCompletedAppointments();

    long getCancelledAppointments();

    BigDecimal getTotalSpentCve();

    Instant getLastAppointmentAt();
}
