package cv.perola.modules.schedule.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor determinístico puro de cálculo de slots de disponibilidade (BE-012).
 *
 * <p>Não depende de base de dados nem do módulo Booking: recebe janelas de expediente,
 * pausas, ausências e intervalos ocupados e devolve os blocos discretos livres.</p>
 */
@Component
public class SlotCalculationEngine {

    public List<TimeInterval> calculateSlots(SlotCalculationInput input) {
        if (input == null
                || input.targetDate() == null
                || input.timezone() == null
                || input.serviceDurationMinutes() <= 0
                || !input.monthOpened()
                || input.allDayTimeOff()
                || !input.workingDay()
                || input.shiftStartTime() == null
                || input.shiftEndTime() == null
                || !input.shiftEndTime().isAfter(input.shiftStartTime())) {
            return List.of();
        }

        LocalDate date = input.targetDate();
        ZoneId zone = input.timezone();
        Instant shiftStart = ZonedDateTime.of(date, input.shiftStartTime(), zone).toInstant();
        Instant shiftEnd = ZonedDateTime.of(date, input.shiftEndTime(), zone).toInstant();
        Instant referenceNow = input.referenceNow();

        if (referenceNow != null && !shiftEnd.isAfter(referenceNow)) {
            return List.of();
        }

        List<TimeInterval> blockers = buildBlockers(input, date, zone);
        Duration slotDuration = Duration.ofMinutes(input.serviceDurationMinutes());
        List<TimeInterval> availableSlots = new ArrayList<>();

        Instant cursor = shiftStart;
        while (!cursor.plus(slotDuration).isAfter(shiftEnd)) {
            Instant candidateEnd = cursor.plus(slotDuration);
            Instant latestOverlapEnd = findLatestOverlappingBlockerEnd(cursor, candidateEnd, blockers);

            if (latestOverlapEnd != null) {
                cursor = latestOverlapEnd;
                continue;
            }

            if (referenceNow == null || !cursor.isBefore(referenceNow)) {
                availableSlots.add(new TimeInterval(cursor, candidateEnd));
            }
            cursor = candidateEnd;
        }

        return List.copyOf(availableSlots);
    }

    private List<TimeInterval> buildBlockers(SlotCalculationInput input, LocalDate date, ZoneId zone) {
        List<TimeInterval> blockers = new ArrayList<>();

        if (input.breaks() != null) {
            for (LocalTimeWindow b : input.breaks()) {
                if (b != null && b.startTime() != null && b.endTime() != null && b.endTime().isAfter(b.startTime())) {
                    blockers.add(new TimeInterval(
                            ZonedDateTime.of(date, b.startTime(), zone).toInstant(),
                            ZonedDateTime.of(date, b.endTime(), zone).toInstant()
                    ));
                }
            }
        }

        if (input.partialTimeOffs() != null) {
            for (LocalTimeWindow t : input.partialTimeOffs()) {
                if (t != null && t.startTime() != null && t.endTime() != null && t.endTime().isAfter(t.startTime())) {
                    blockers.add(new TimeInterval(
                            ZonedDateTime.of(date, t.startTime(), zone).toInstant(),
                            ZonedDateTime.of(date, t.endTime(), zone).toInstant()
                    ));
                }
            }
        }

        if (input.bookedIntervals() != null) {
            for (TimeInterval a : input.bookedIntervals()) {
                if (a != null && a.startTime() != null && a.endTime() != null && a.endTime().isAfter(a.startTime())) {
                    blockers.add(a);
                }
            }
        }

        blockers.sort(Comparator.comparing(TimeInterval::startTime).thenComparing(TimeInterval::endTime));
        return blockers;
    }

    private Instant findLatestOverlappingBlockerEnd(Instant start, Instant end, List<TimeInterval> blockers) {
        Instant latestEnd = null;
        for (TimeInterval blocker : blockers) {
            if (blocker.overlaps(start, end)) {
                if (latestEnd == null || blocker.endTime().isAfter(latestEnd)) {
                    latestEnd = blocker.endTime();
                }
            }
        }
        return latestEnd;
    }
}

