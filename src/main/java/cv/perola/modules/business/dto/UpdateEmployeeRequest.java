package cv.perola.modules.business.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para atualização de dados de um profissional do salão")
public record UpdateEmployeeRequest(

        @Schema(description = "Nome de exibição pública do profissional", example = "Maria Silva - Estilista Sénior")
        @Size(max = 150, message = "O nome de exibição não pode exceder 150 caracteres")
        String displayName,

        @Schema(description = "Título ou especialidade exibida no perfil do profissional", example = "Barbeiro Sénior", nullable = true)
        @Size(max = 120, message = "A especialidade não pode exceder 120 caracteres")
        String specialty,

        @Schema(description = "URL ou storageKey da foto do profissional obtida via POST /api/v1/media/upload (scope EMPLOYEE_AVATAR)", nullable = true)
        @Size(max = 1000, message = "O URL do avatar não pode exceder 1000 caracteres")
        String avatarUrl,

        @Schema(description = "Estado de ativação do profissional", example = "true")
        Boolean isActive
) {
    @JsonCreator
    public UpdateEmployeeRequest {
    }

    public UpdateEmployeeRequest(String displayName, String specialty, Boolean isActive) {
        this(displayName, specialty, null, isActive);
    }
}
