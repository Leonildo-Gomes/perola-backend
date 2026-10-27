package cv.perola.modules.schedule.dto;

import cv.perola.modules.schedule.domain.enums.TimeOffType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Dados do registo de ausência ou bloqueio de agenda")
public record TimeOffResponse(

        @Schema(description = "Identificador único da ausência", example = "9a8b7c6d-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador do colaborador", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Data de início da ausência (YYYY-MM-DD)", example = "2026-11-10")
        LocalDate startDate,

        @Schema(description = "Data de término da ausência (YYYY-MM-DD)", example = "2026-11-15")
        LocalDate endDate,

        @Schema(description = "Indica se o bloqueio abrange o dia inteiro", example = "true")
        boolean isAllDay,

        @Schema(description = "Horário de início (se parcial)", example = "14:00")
        LocalTime startTime,

        @Schema(description = "Horário de término (se parcial)", example = "18:00")
        LocalTime endTime,

        @Schema(description = "Tipo de ausência", example = "VACATION")
        TimeOffType type,

        @Schema(description = "Observação ou motivo", example = "Férias anuais")
        String note,

        @Schema(description = "Data de criação em UTC", example = "2026-10-27T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Data da última atualização em UTC", example = "2026-10-27T10:00:00Z")
        Instant updatedAt
) {
}
