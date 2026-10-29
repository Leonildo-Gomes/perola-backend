package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Payload para abertura da agenda mensal do colaborador")
public record OpenMonthlyScheduleRequest(

        @NotNull(message = "O identificador do perfil de horário (scheduleId) é obrigatório.")
        @Schema(description = "Identificador do perfil de horário semanal que vigorará neste mês", example = "9a8b7c6d-5717-4562-b3fc-2c963f66afa6")
        UUID scheduleId,

        @NotNull(message = "O ano de referência é obrigatório.")
        @Min(value = 2024, message = "O ano deve ser igual ou superior a 2024.")
        @Max(value = 2100, message = "O ano deve ser igual ou inferior a 2100.")
        @Schema(description = "Ano de referência", example = "2026")
        Integer year,

        @NotNull(message = "O mês de referência é obrigatório.")
        @Min(value = 1, message = "O mês deve estar entre 1 e 12.")
        @Max(value = 12, message = "O mês deve estar entre 1 e 12.")
        @Schema(description = "Mês do calendário (1..12)", example = "11")
        Integer month,

        @Valid
        @Schema(description = "Lista opcional de decisões para feriados do mês")
        List<HolidayDecisionRequest> holidayDecisions
) {
}

