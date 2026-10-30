package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Notas opcionais associadas a uma ação sobre o agendamento")
public record AppointmentActionNotesRequest(
        @Schema(description = "Observações opcionais sobre a ação realizada")
        @Size(max = 1000, message = "As notas não podem exceder 1000 caracteres.")
        String notes
) {
}

