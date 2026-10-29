package cv.perola.modules.schedule.dto;

import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados do expediente e pausas de um dia específico da semana")
public record ScheduleDayItemResponse(

        @Schema(description = "Identificador único do dia de horário", example = "5f6e7d8c-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Dia da semana", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @Schema(description = "Indica se o profissional trabalha neste dia", example = "true")
        boolean isWorking,

        @Schema(description = "Horário de início do turno (HH:mm)", example = "08:30", nullable = true)
        LocalTime startTime,

        @Schema(description = "Horário de término do turno (HH:mm)", example = "16:00", nullable = true)
        LocalTime endTime,

        @Schema(description = "Lista de pausas configuradas para este dia")
        List<BreakItemResponse> breaks
) {
}

