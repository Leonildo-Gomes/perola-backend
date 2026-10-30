package cv.perola.modules.booking.service;

import cv.perola.modules.schedule.service.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcBookedIntervalProviderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcBookedIntervalProvider provider;

    @Test
    @DisplayName("Deve consultar intervalos ocupados no banco para o profissional e janela informados")
    @SuppressWarnings("unchecked")
    void shouldQueryBookedIntervalsFromDatabase() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        Instant windowStart = Instant.parse("2026-11-03T09:00:00Z");
        Instant windowEnd = Instant.parse("2026-11-03T18:00:00Z");
        TimeInterval expectedInterval = new TimeInterval(
                Instant.parse("2026-11-03T10:00:00Z"),
                Instant.parse("2026-11-03T11:00:00Z")
        );
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(employeeId),
                eq(Timestamp.from(windowEnd)),
                eq(Timestamp.from(windowStart))
        )).thenReturn(List.of(expectedInterval));

        // Act
        List<TimeInterval> result = provider.findBookedIntervals(employeeId, windowStart, windowEnd);

        // Assert
        assertThat(result).containsExactly(expectedInterval);
        verify(jdbcTemplate).query(
                anyString(),
                any(RowMapper.class),
                eq(employeeId),
                eq(Timestamp.from(windowEnd)),
                eq(Timestamp.from(windowStart))
        );
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando parâmetros forem nulos sem consultar o banco")
    void shouldReturnEmptyListWhenParametersAreNull() {
        // Arrange
        Instant now = Instant.now();

        // Act
        List<TimeInterval> result = provider.findBookedIntervals(null, now, now.plusSeconds(3600));

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }
}

