package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.dto.AvailabilitySlotsResponse;
import cv.perola.modules.schedule.dto.AvailableSlotResponse;
import cv.perola.modules.schedule.service.TimeInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityMapperTest {

    private static final ZoneId CAPE_VERDE_ZONE = ZoneId.of("Atlantic/Cape_Verde");

    private AvailabilityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AvailabilityMapper();
    }

    @Test
    @DisplayName("Deve converter TimeInterval em AvailableSlotResponse com offset do fuso horário do salão")
    void shouldMapTimeIntervalToAvailableSlotResponseWithBusinessOffset() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 11, 3);
        TimeInterval interval = new TimeInterval(
                ZonedDateTime.of(date, LocalTime.of(8, 30), CAPE_VERDE_ZONE).toInstant(),
                ZonedDateTime.of(date, LocalTime.of(9, 30), CAPE_VERDE_ZONE).toInstant()
        );

        // Act
        AvailableSlotResponse slot = mapper.toSlotResponse(interval, CAPE_VERDE_ZONE);

        // Assert
        assertThat(slot.startTime().getOffset()).isEqualTo(ZoneOffset.ofHours(-1));
        assertThat(slot.startTime().toLocalTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(slot.endTime().toLocalTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Deve converter lista de TimeIntervals em AvailabilitySlotsResponse completo")
    void shouldMapIntervalsListToAvailabilitySlotsResponse() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 11, 3);
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        List<TimeInterval> intervals = List.of(
                new TimeInterval(
                        ZonedDateTime.of(date, LocalTime.of(9, 0), CAPE_VERDE_ZONE).toInstant(),
                        ZonedDateTime.of(date, LocalTime.of(10, 0), CAPE_VERDE_ZONE).toInstant()
                )
        );

        // Act
        AvailabilitySlotsResponse response = mapper.toResponse(
                date, employeeId, serviceId, 60, intervals, CAPE_VERDE_ZONE
        );

        // Assert
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.serviceId()).isEqualTo(serviceId);
        assertThat(response.durationMinutes()).isEqualTo(60);
        assertThat(response.slots()).hasSize(1);
    }
}

