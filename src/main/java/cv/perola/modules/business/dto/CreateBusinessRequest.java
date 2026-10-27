package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação e registo de um novo salão")
public record CreateBusinessRequest(
        @NotBlank(message = "O nome do estabelecimento é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        @Schema(description = "Nome comercial do salão", example = "Salão Pérola da Praia")
        String name,

        @NotBlank(message = "A ilha é obrigatória.")
        @Size(max = 50, message = "A ilha deve ter no máximo 50 caracteres.")
        @Schema(description = "Ilha de Cabo Verde", example = "Santiago")
        String island,

        @NotBlank(message = "A cidade/concelho é obrigatória.")
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
        @Schema(description = "Cidade ou concelho", example = "Praia")
        String city,

        @NotBlank(message = "A localização/morada é obrigatória.")
        @Schema(description = "Morada ou ponto de referência", example = "Rua Pedonal do Platô, nº 12")
        String location,

        @NotBlank(message = "O telefone de contacto é obrigatório.")
        @Size(max = 50, message = "O telefone deve ter no máximo 50 caracteres.")
        @Schema(description = "Telefone público de contacto", example = "+2382619988")
        String contactPhone,

        @Schema(description = "Fuso horário IANA (padrão Atlantic/Cape_Verde)", example = "Atlantic/Cape_Verde")
        String timezone
) {
}
