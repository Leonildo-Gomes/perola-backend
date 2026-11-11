package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Categoria associada a um estabelecimento")
public record BusinessCategoryResponse(
        @Schema(description = "Identificador único da categoria")
        UUID id,

        @Schema(description = "Slug único da categoria", example = "spa_massage")
        String slug,

        @Schema(description = "Nome de exibição", example = "Spa & Massagem")
        String name,

        @Schema(description = "Ícone UI/Lucide", example = "Flower2", nullable = true)
        String icon,

        @Schema(description = "Indica se é a categoria principal exibida em destaque", example = "true")
        boolean isPrimary
) {
}

