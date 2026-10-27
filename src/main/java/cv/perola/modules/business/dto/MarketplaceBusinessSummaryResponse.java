package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resumo público de um salão para listagem e busca no marketplace")
public record MarketplaceBusinessSummaryResponse(

        @Schema(description = "Identificador único do salão", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nome do salão", example = "Salão Pérola do Platô")
        String name,

        @Schema(description = "Ilha", example = "Santiago")
        String island,

        @Schema(description = "Cidade ou Concelho", example = "Praia")
        String city,

        @Schema(description = "Morada ou Ponto de Referência", example = "Rua Pedonal, Platô")
        String location,

        @Schema(description = "Telefone de contacto público", example = "+2382619988")
        String contactPhone,

        @Schema(description = "Fuso horário do salão", example = "Atlantic/Cape_Verde")
        String timezone,

        @Schema(description = "Quantidade de serviços ativos disponíveis no salão", example = "12")
        long servicesCount
) {
}
