package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para atualização de dados de um profissional do salão")
public record UpdateEmployeeRequest(

        @Schema(description = "Nome de exibição pública do profissional", example = "Maria Silva - Estilista Sénior")
        @Size(max = 150, message = "O nome de exibição não pode exceder 150 caracteres")
        String displayName,

        @Schema(description = "Estado de ativação do profissional", example = "true")
        Boolean isActive
) {
}
