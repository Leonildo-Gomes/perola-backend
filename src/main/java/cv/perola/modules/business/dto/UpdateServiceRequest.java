package cv.perola.modules.business.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Payload para atualização de um serviço do catálogo")
public record UpdateServiceRequest(

                @Schema(description = "UUID da categoria do serviço", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID categoryId,

                @Schema(description = "Nome do serviço", example = "Corte Feminino & Brushing") @Size(max = 150, message = "O nome do serviço não pode exceder 150 caracteres") String name,

                @Schema(description = "Descrição detalhada do serviço", example = "Corte, hidratação rápida e brushing modelado") @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres") String description,

                @Schema(description = "Duração estimada em minutos", example = "75") @Min(value = 5, message = "A duração mínima do serviço é de 5 minutos") Integer durationMinutes,

                @Schema(description = "Preço do serviço em CVE", example = "3000.00") @DecimalMin(value = "0.00", message = "O preço não pode ser negativo") BigDecimal price,

                @Schema(description = "URL ou storageKey da foto do serviço obtida via POST /api/v1/media/upload (scope SERVICE_IMAGE)", nullable = true) @Size(max = 1000, message = "O URL da imagem não pode exceder 1000 caracteres") String imageUrl,

                @Schema(description = "Estado de ativação do serviço", example = "true") Boolean isActive) {
        @JsonCreator
        public UpdateServiceRequest {
        }

        public UpdateServiceRequest(
                        UUID categoryId,
                        String name,
                        String description,
                        Integer durationMinutes,
                        BigDecimal price,
                        Boolean isActive) {
                this(categoryId, name, description, durationMinutes, price, null, isActive);
        }
}
