package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Payload para adicionar um novo profissional à equipa do salão")
public record CreateEmployeeRequest(

        @Schema(description = "Identificador do utilizador associado (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "Nome de exibição pública do profissional", example = "Maria Silva")
        @NotBlank(message = "O nome de exibição é obrigatório")
        @Size(max = 150, message = "O nome de exibição não pode exceder 150 caracteres")
        String displayName
) {
}
