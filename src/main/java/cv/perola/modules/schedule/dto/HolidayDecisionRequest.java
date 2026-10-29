package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Decisão de expediente ou folga para um feriado específico do mês")
public record HolidayDecisionRequest(

        @NotNull(message = "A data do feriado é obrigatória.")
        @Schema(description = "Data do feriado (YYYY-MM-DD)", example = "2026-11-01")
        LocalDate holidayDate,

        @NotNull(message = "A decisão sobre trabalhar no feriado é obrigatória.")
        @Schema(description = "true se o profissional irá trabalhar no feriado; false para criar folga integral (HOLIDAY)", example = "false")
        Boolean workOnHoliday
) {
}

