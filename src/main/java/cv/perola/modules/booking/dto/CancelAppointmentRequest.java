package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido de cancelamento de agendamento")
public record CancelAppointmentRequest(
        @Schema(description = "Motivo do cancelamento", example = "Imprevisto pessoal.")
        @Size(max = 1000, message = "O motivo não pode exceder 1000 caracteres.")
        String reason
) {
}

