package cv.perola.modules.schedule.service;

import java.time.LocalTime;

/**
 * Intervalo de hora local dentro de um dia (ex.: pausa de almoço ou ausência parcial).
 */
public record LocalTimeWindow(
        LocalTime startTime,
        LocalTime endTime
) {
}

