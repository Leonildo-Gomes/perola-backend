package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "Pedido de proposta de reagendamento")
public record RescheduleAppointmentRequest(
        @Schema(description = "Nova data e hora de início proposta (ISO-8601 com offset)", example = "2026-11-05T15:00:00-01:00")
        @NotNull(message = "A nova data e hora propostas (proposedStartTime) são obrigatórias.")
        OffsetDateTime proposedStartTime,

        @Schema(description = "Motivo ou detalhes do pedido de reagendamento", example = "Imprevisto de trabalho na data anterior.")
        @Size(max = 1000, message = "O motivo não pode exceder 1000 caracteres.")
        String reason
) {
}

