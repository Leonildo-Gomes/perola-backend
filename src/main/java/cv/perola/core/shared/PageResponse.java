package cv.perola.core.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Estrutura padrão de resposta paginada")
public record PageResponse<T>(

        @Schema(description = "Lista de elementos da página atual")
        List<T> content,

        @Schema(description = "Número da página atual (0-based)", example = "0")
        int page,

        @Schema(description = "Tamanho da página", example = "20")
        int size,

        @Schema(description = "Total de elementos encontrados", example = "42")
        long totalElements,

        @Schema(description = "Total de páginas disponíveis", example = "3")
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
