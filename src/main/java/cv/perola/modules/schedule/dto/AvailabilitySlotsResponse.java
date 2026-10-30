package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Resposta contendo os slots disponíveis para um profissional, serviço e data")
public record AvailabilitySlotsResponse(
        @Schema(description = "Data consultada", example = "2026-11-03")
        LocalDate date,

        @Schema(description = "Identificador do profissional", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Identificador do serviço", example = "9f8e7d6c-5717-4562-b3fc-2c963f66afa6")
        UUID serviceId,

        @Schema(description = "Duração do serviço em minutos", example = "60")
        int durationMinutes,

        @Schema(description = "Lista de slots livres para agendamento")
        List<AvailableSlotResponse> slots
) {
}

