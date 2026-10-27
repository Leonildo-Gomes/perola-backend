package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload para criação de um novo serviço no catálogo do salão")
public record CreateServiceRequest(

        @Schema(description = "Nome do serviço", example = "Corte & Brushing")
        @NotBlank(message = "O nome do serviço é obrigatório")
        @Size(max = 150, message = "O nome do serviço não pode exceder 150 caracteres")
        String name,

        @Schema(description = "Descrição detalhada do serviço", example = "Lavagem, corte profissional e modelagem com escova")
        @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
        String description,

        @Schema(description = "Duração estimada em minutos", example = "60")
        @NotNull(message = "A duração do serviço é obrigatória")
        @Min(value = 5, message = "A duração mínima do serviço é de 5 minutos")
        Integer durationMinutes,

        @Schema(description = "Preço do serviço em CVE", example = "2500.00")
        @NotNull(message = "O preço do serviço é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        BigDecimal price
) {
}
