package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.dto.AvailabilitySlotsResponse;
import cv.perola.modules.schedule.dto.AvailableSlotResponse;
import cv.perola.modules.schedule.service.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Mapper dedicado para conversão de intervalos calculados para DTOs de disponibilidade.
 */
@Component
public class AvailabilityMapper {

    public AvailableSlotResponse toSlotResponse(TimeInterval interval, ZoneId zoneId) {
        return new AvailableSlotResponse(
                OffsetDateTime.ofInstant(interval.startTime(), zoneId),
                OffsetDateTime.ofInstant(interval.endTime(), zoneId)
        );
    }

    public AvailabilitySlotsResponse toResponse(
            LocalDate date,
            UUID employeeId,
            UUID serviceId,
            int durationMinutes,
            List<TimeInterval> intervals,
            ZoneId zoneId
    ) {
        List<AvailableSlotResponse> slots = intervals == null
                ? List.of()
                : intervals.stream()
                        .map(interval -> toSlotResponse(interval, zoneId))
                        .toList();

        return new AvailabilitySlotsResponse(
                date,
                employeeId,
                serviceId,
                durationMinutes,
                slots
        );
    }
}

