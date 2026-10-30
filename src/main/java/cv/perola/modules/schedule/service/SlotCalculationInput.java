package cv.perola.modules.schedule.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Parâmetros de entrada imutáveis para o cálculo determinístico de slots livres.
 */
public record SlotCalculationInput(
        LocalDate targetDate,
        ZoneId timezone,
        int serviceDurationMinutes,
        boolean monthOpened,
        boolean workingDay,
        LocalTime shiftStartTime,
        LocalTime shiftEndTime,
        List<LocalTimeWindow> breaks,
        boolean allDayTimeOff,
        List<LocalTimeWindow> partialTimeOffs,
        List<TimeInterval> bookedIntervals,
        Instant referenceNow
) {
}

