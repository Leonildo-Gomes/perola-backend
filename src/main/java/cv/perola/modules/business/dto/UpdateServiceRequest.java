package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload para atualização parcial de um serviço do catálogo")
public record UpdateServiceRequest(

        @Schema(description = "Nome do serviço", example = "Corte Feminino & Brushing")
        @Size(max = 150, message = "O nome do serviço não pode exceder 150 caracteres")
        String name,

        @Schema(description = "Descrição detalhada do serviço", example = "Corte, hidratação rápida e brushing modelado")
        @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
        String description,

        @Schema(description = "Duração estimada em minutos", example = "75")
        @Min(value = 5, message = "A duração mínima do serviço é de 5 minutos")
        Integer durationMinutes,

        @Schema(description = "Preço do serviço em CVE", example = "3000.00")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        BigDecimal price,

        @Schema(description = "Estado de ativação do serviço", example = "true")
        Boolean isActive
) {
}
