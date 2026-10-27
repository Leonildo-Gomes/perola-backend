package cv.perola.modules.schedule.dto;

import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "Configuração do expediente para um dia específico da semana")
public record WeeklyScheduleItemRequest(

        @NotNull(message = "O dia da semana é obrigatório.")
        @Schema(description = "Dia da semana", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @NotNull(message = "O horário de início do expediente é obrigatório.")
        @Schema(description = "Horário de início do turno (HH:mm)", example = "08:00")
        LocalTime startTime,

        @NotNull(message = "O horário de término do expediente é obrigatório.")
        @Schema(description = "Horário de término do turno (HH:mm)", example = "18:00")
        LocalTime endTime,

        @Schema(description = "Indica se o profissional trabalha neste dia", example = "true", defaultValue = "true")
        Boolean isActive,

        @Valid
        @Schema(description = "Lista de pausas ou intervalos no dia")
        List<BreakItemRequest> breaks
) {
    public boolean resolvedIsActive() {
        return isActive == null || isActive;
    }
}
