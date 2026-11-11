package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Item de associação de categoria ao estabelecimento")
public record BusinessCategoryItemRequest(
        @NotNull(message = "O identificador da categoria é obrigatório.")
        @Schema(description = "UUID da categoria", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @NotNull(message = "A indicação de categoria principal (isPrimary) é obrigatória.")
        @Schema(description = "Define se esta é a categoria principal do estabelecimento (exatamente 1 deve ser true)", example = "true")
        Boolean isPrimary
) {
}

