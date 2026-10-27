package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@Schema(description = "Dados para registo de uma pausa durante o expediente de trabalho")
public record BreakItemRequest(

        @NotBlank(message = "O título da pausa é obrigatório.")
        @Size(max = 100, message = "O título não pode exceder 100 caracteres.")
        @Schema(description = "Identificador ou motivo da pausa", example = "Almoço")
        String title,

        @NotNull(message = "O horário de início da pausa é obrigatório.")
        @Schema(description = "Horário de início da pausa (HH:mm)", example = "12:00")
        LocalTime startTime,

        @NotNull(message = "O horário de término da pausa é obrigatório.")
        @Schema(description = "Horário de término da pausa (HH:mm)", example = "13:00")
        LocalTime endTime
) {
}
