package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Pedido de criação de uma nova marcação")
public record CreateAppointmentRequest(
        @Schema(description = "Identificador único do salão", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "O identificador do salão é obrigatório.")
        UUID businessId,

        @Schema(description = "Identificador único do profissional", example = "223e4567-e89b-12d3-a456-426614174001")
        @NotNull(message = "O identificador do profissional é obrigatório.")
        UUID employeeId,

        @Schema(description = "Identificador único do serviço", example = "323e4567-e89b-12d3-a456-426614174002")
        @NotNull(message = "O identificador do serviço é obrigatório.")
        UUID serviceId,

        @Schema(description = "Data e hora de início pretendida (ISO-8601 com offset)", example = "2026-11-03T10:00:00-01:00")
        @NotNull(message = "A data e hora de início são obrigatórias.")
        OffsetDateTime startTime,

        @Schema(description = "Notas ou observações opcionais do cliente", example = "Prefiro atendimento pontual.")
        @Size(max = 1000, message = "As notas não podem exceder 1000 caracteres.")
        String notes
) {
}

