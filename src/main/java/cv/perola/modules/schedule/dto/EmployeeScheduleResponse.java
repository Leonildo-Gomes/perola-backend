package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados completos de um perfil de horário semanal do profissional (cabeçalho, 7 dias e pausas)")
public record EmployeeScheduleResponse(

        @Schema(description = "Identificador único do perfil de horário", example = "9a8b7c6d-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador do profissional", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Nome do perfil de horário", example = "Horário Padrão")
        String name,

        @Schema(description = "Indica se é o horário padrão pré-selecionado", example = "true")
        boolean isDefault,

        @Schema(description = "Indica se o perfil de horário está ativo", example = "true")
        boolean isActive,

        @Schema(description = "Lista dos 7 dias da semana configurados neste perfil")
        List<ScheduleDayItemResponse> days,

        @Schema(description = "Data/hora de criação")
        Instant createdAt,

        @Schema(description = "Data/hora da última atualização")
        Instant updatedAt
) {
}

