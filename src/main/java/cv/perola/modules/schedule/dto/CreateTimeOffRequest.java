package cv.perola.modules.schedule.dto;

import cv.perola.modules.schedule.domain.enums.TimeOffType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Pedido de registo de ausência, férias ou bloqueio pontual de horário")
public record CreateTimeOffRequest(

        @NotNull(message = "A data de início é obrigatória.")
        @Schema(description = "Data de início da ausência (YYYY-MM-DD)", example = "2026-11-10")
        LocalDate startDate,

        @NotNull(message = "A data de término é obrigatória.")
        @Schema(description = "Data de término da ausência (YYYY-MM-DD)", example = "2026-11-15")
        LocalDate endDate,

        @Schema(description = "Indica se o bloqueio abrange o dia inteiro", example = "true", defaultValue = "true")
        Boolean isAllDay,

        @Schema(description = "Horário de início (obrigatório se isAllDay = false)", example = "14:00")
        LocalTime startTime,

        @Schema(description = "Horário de término (obrigatório se isAllDay = false)", example = "18:00")
        LocalTime endTime,

        @NotNull(message = "O tipo de ausência é obrigatório.")
        @Schema(description = "Tipo de ausência", example = "VACATION")
        TimeOffType type,

        @Size(max = 500, message = "A observação não pode exceder 500 caracteres.")
        @Schema(description = "Justificação ou motivo da ausência", example = "Férias anuais")
        String note
) {
    public boolean resolvedIsAllDay() {
        return isAllDay == null || isAllDay;
    }
}
