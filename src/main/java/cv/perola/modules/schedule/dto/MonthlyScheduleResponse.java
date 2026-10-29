package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados de abertura mensal da agenda do colaborador e horário semanal associado")
public record MonthlyScheduleResponse(

        @Schema(description = "Identificador único do registo de abertura mensal", example = "3c4d5e6f-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador do profissional", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Identificador do perfil de horário semanal associado a este mês", example = "9a8b7c6d-5717-4562-b3fc-2c963f66afa6")
        UUID scheduleId,

        @Schema(description = "Nome do perfil de horário semanal associado", example = "Horário Padrão")
        String scheduleName,

        @Schema(description = "Ano de referência", example = "2026")
        int year,

        @Schema(description = "Mês de referência (1..12)", example = "11")
        int month,

        @Schema(description = "Indica se a agenda mensal está aberta para marcações", example = "true")
        boolean isOpened,

        @Schema(description = "Momento em que a agenda mensal foi aberta")
        Instant openedAt
) {
}

