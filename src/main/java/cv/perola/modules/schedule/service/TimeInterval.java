package cv.perola.modules.schedule.service;

import java.time.Instant;

/**
 * Intervalo absoluto semi-aberto [startTime, endTime) em UTC.
 */
public record TimeInterval(
        Instant startTime,
        Instant endTime
) {

    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return this.startTime.isBefore(otherEnd) && otherStart.isBefore(this.endTime);
    }
}

