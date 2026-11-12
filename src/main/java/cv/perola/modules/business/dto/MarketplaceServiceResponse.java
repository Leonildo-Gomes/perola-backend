package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Serviço listado na busca pública do marketplace com informações do salão")
public record MarketplaceServiceResponse(

        @Schema(description = "Identificador único do serviço", example = "9f8e7d6c-5717-4562-b3fc-2c963f66afa6") UUID id,

        @Schema(description = "Identificador do salão detentor", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID businessId,

        @Schema(description = "Identificador da categoria do serviço", example = "2fa85f64-5717-4562-b3fc-2c963f66afa6") UUID categoryId,

        @Schema(description = "Slug da categoria do serviço", example = "hair") String categorySlug,

        @Schema(description = "Nome da categoria do serviço", example = "Cabeleireiro & Tranças") String categoryName,

        @Schema(description = "Nome do salão", example = "Salão Pérola do Platô") String businessName,

        @Schema(description = "Ilha do salão", example = "Santiago") String businessIsland,

        @Schema(description = "Cidade do salão", example = "Praia") String businessCity,

        @Schema(description = "Nome do serviço", example = "Corte & Brushing") String name,

        @Schema(description = "Descrição detalhada do serviço", example = "Lavagem, corte profissional e modelagem com escova") String description,

        @Schema(description = "Duração estimada em minutos", example = "60") Integer durationMinutes,

        @Schema(description = "Preço do serviço em CVE", example = "2500.00")
BigDecimal price,

@Schema(description = "URL da foto do serviço", nullable = true)
String imageUrl) {

    public MarketplaceServiceResponse(
            UUID id,
            UUID businessId,
            UUID categoryId,
            String categorySlug,
            String categoryName,
            String businessName,
            String businessIsland,
            String businessCity,
            String name,
            String description,
            Integer durationMinutes,
            BigDecimal price) {
        this(id, businessId, categoryId, categorySlug, categoryName, businessName, businessIsland, businessCity, name,
                description, durationMinutes, price, null);
    }
}
