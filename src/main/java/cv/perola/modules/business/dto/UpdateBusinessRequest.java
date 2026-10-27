package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização das informações do salão")
public record UpdateBusinessRequest(
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        @Schema(description = "Novo nome comercial do salão", example = "Salão Pérola VIP")
        String name,

        @Size(max = 50, message = "A ilha deve ter no máximo 50 caracteres.")
        @Schema(description = "Ilha de Cabo Verde", example = "Santiago")
        String island,

        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
        @Schema(description = "Cidade ou concelho", example = "Praia")
        String city,

        @Schema(description = "Nova morada ou ponto de referência", example = "Avenida Cidade de Lisboa, Edifício Pérola")
        String location,

        @Size(max = 50, message = "O telefone deve ter no máximo 50 caracteres.")
        @Schema(description = "Telefone público de contacto", example = "+2382619999")
        String contactPhone,

        @Schema(description = "Fuso horário IANA", example = "Atlantic/Cape_Verde")
        String timezone
) {
}
