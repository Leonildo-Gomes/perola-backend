package cv.perola.modules.booking.dto;

import cv.perola.modules.booking.domain.enums.CustomerTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido de atualização da classificação (tier) e notas internas de um cliente do salão")
public record UpdateBusinessCustomerRequest(
        @Schema(description = "Nova classificação CRM do cliente", example = "VIP")
        CustomerTier tier,

        @Schema(description = "Notas internas privadas da equipa sobre o cliente", example = "Cliente VIP. Prefere atendimento com a Helena e café expresso.")
        @Size(max = 2000, message = "As notas internas não podem exceder 2000 caracteres.")
        String internalNotes
) {
}
