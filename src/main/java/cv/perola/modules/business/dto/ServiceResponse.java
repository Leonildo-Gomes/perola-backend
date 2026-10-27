package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados do serviço no catálogo do salão")
public record ServiceResponse(

        @Schema(description = "Identificador único do serviço", example = "9f8e7d6c-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador do salão detentor do serviço", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID businessId,

        @Schema(description = "Nome do serviço", example = "Corte & Brushing")
        String name,

        @Schema(description = "Descrição detalhada do serviço", example = "Lavagem, corte profissional e modelagem com escova")
        String description,

        @Schema(description = "Duração estimada em minutos", example = "60")
        Integer durationMinutes,

        @Schema(description = "Preço do serviço em CVE", example = "2500.00")
        BigDecimal price,

        @Schema(description = "Estado de ativação do serviço", example = "true")
        boolean isActive,

        @Schema(description = "Data e hora de criação em UTC", example = "2026-10-27T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Data e hora da última atualização em UTC", example = "2026-10-27T10:00:00Z")
        Instant updatedAt
) {
}
