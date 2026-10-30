package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido de recusa de proposta de reagendamento")
public record RejectRescheduleRequest(
        @Schema(description = "Motivo da recusa do reagendamento", example = "Agenda cheia no horário solicitado.")
        @Size(max = 1000, message = "O motivo não pode exceder 1000 caracteres.")
        String reason
) {
}

