package cv.perola.modules.schedule.dto;

import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "Configuração do expediente e pausas para um dia da semana")
public record ScheduleDayItemRequest(

        @NotNull(message = "O dia da semana é obrigatório.")
        @Schema(description = "Dia da semana (MONDAY..SUNDAY)", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @Schema(description = "Indica se o profissional trabalha neste dia", example = "true", defaultValue = "true")
        Boolean isWorking,

        @Schema(description = "Horário de início do turno (HH:mm) — obrigatório quando isWorking=true, nulo quando isWorking=false", example = "08:30")
        LocalTime startTime,

        @Schema(description = "Horário de término do turno (HH:mm) — obrigatório quando isWorking=true, nulo quando isWorking=false", example = "16:00")
        LocalTime endTime,

        @Valid
        @Schema(description = "Lista de pausas ou intervalos no dia (apenas permitido quando isWorking=true)")
        List<BreakItemRequest> breaks
) {
    public boolean resolvedIsWorking() {
        return isWorking == null || isWorking;
    }
}

