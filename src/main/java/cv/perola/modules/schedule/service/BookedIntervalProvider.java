package cv.perola.modules.schedule.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Porta de saída (SPI) para obtenção de intervalos ocupados por marcações ativas,
 * mantendo o módulo Schedule desacoplado do módulo Booking (sem dependência cíclica).
 */
public interface BookedIntervalProvider {

    /**
     * Retorna os intervalos de tempo ocupados por marcações ativas
     * (PENDING, CONFIRMED, RESCHEDULE_PENDING) de um profissional que se sobreponham
     * à janela [windowStart, windowEnd).
     */
    List<TimeInterval> findBookedIntervals(UUID employeeId, Instant windowStart, Instant windowEnd);
}

