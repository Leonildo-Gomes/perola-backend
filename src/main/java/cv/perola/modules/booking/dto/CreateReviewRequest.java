package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de requisição para submissão de avaliação pós-serviço")
public record CreateReviewRequest(

        @Schema(description = "Nota de avaliação de 1 a 5 estrelas", example = "5", minimum = "1", maximum = "5")
        @NotNull(message = "A nota de avaliação (rating) é obrigatória")
        @Min(value = 1, message = "A nota mínima permitida é 1")
        @Max(value = 5, message = "A nota máxima permitida é 5")
        Integer rating,

        @Schema(description = "Comentário opcional sobre o atendimento", example = "Excelente atendimento, super pontual e profissional!")
        @Size(max = 1000, message = "O comentário não pode exceder 1000 caracteres")
        String comment
) {
}

