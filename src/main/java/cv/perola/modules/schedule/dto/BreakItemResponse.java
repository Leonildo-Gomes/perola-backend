package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Dados de uma pausa configurada")
public record BreakItemResponse(

        @Schema(description = "Identificador único da pausa", example = "a1b2c3d4-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Título ou descrição da pausa", example = "Almoço")
        String title,

        @Schema(description = "Horário de início da pausa (HH:mm)", example = "12:00")
        LocalTime startTime,

        @Schema(description = "Horário de término da pausa (HH:mm)", example = "13:00")
        LocalTime endTime
) {
}
