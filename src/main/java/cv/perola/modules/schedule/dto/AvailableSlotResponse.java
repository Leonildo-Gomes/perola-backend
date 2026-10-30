package cv.perola.modules.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Intervalo de horário livre disponível para agendamento")
public record AvailableSlotResponse(
        @Schema(description = "Data e hora de início do slot em ISO-8601 com offset", example = "2026-11-03T08:00:00-01:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime startTime,

        @Schema(description = "Data e hora de término do slot em ISO-8601 com offset", example = "2026-11-03T09:00:00-01:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime endTime
) {
}

