package cv.perola.modules.booking.service;

import cv.perola.modules.schedule.service.BookedIntervalProvider;
import cv.perola.modules.schedule.service.TimeInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementação no módulo Booking que consulta na tabela appointments os intervalos
 * ocupados por marcações ativas (PENDING, CONFIRMED, RESCHEDULE_PENDING).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdbcBookedIntervalProvider implements BookedIntervalProvider {

    private static final String FIND_BOOKED_INTERVALS_SQL = """
            SELECT start_time, end_time
            FROM appointments
            WHERE employee_id = ?
              AND status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_PENDING')
              AND start_time < ?
              AND end_time > ?
            ORDER BY start_time ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<TimeInterval> findBookedIntervals(UUID employeeId, Instant windowStart, Instant windowEnd) {
        if (employeeId == null || windowStart == null || windowEnd == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                FIND_BOOKED_INTERVALS_SQL,
                (rs, rowNum) -> new TimeInterval(
                        rs.getTimestamp("start_time").toInstant(),
                        rs.getTimestamp("end_time").toInstant()
                ),
                employeeId,
                Timestamp.from(windowEnd),
                Timestamp.from(windowStart)
        );
    }
}

