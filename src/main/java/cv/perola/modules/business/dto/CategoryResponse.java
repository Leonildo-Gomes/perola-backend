package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Categoria global de beleza e bem-estar disponível no marketplace")
public record CategoryResponse(
        @Schema(description = "Identificador único da categoria", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Slug único da categoria", example = "barbershop")
        String slug,

        @Schema(description = "Nome de exibição em português", example = "Barbearia")
        String name,

        @Schema(description = "Nome do ícone UI/Lucide", example = "Scissors", nullable = true)
        String icon,

        @Schema(description = "Ordem de apresentação no marketplace", example = "1")
        Integer displayOrder
) {
}

