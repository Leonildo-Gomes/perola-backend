package cv.perola.modules.business.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Payload para adicionar um novo profissional à equipa do salão")
public record CreateEmployeeRequest(

        @Schema(description = "Identificador do utilizador associado (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID userId,

        @Schema(description = "Nome de exibição pública do profissional", example = "Maria Silva") @NotBlank(message = "O nome de exibição é obrigatório") @Size(max = 150, message = "O nome de exibição não pode exceder 150 caracteres") String displayName,

        @Schema(description = "Título ou especialidade exibida no perfil do profissional", example = "Cabeleireira Sénior & Trancista", nullable = true) @Size(max = 120, message = "A especialidade não pode exceder 120 caracteres")
String specialty,

@Schema(description = "URL ou storageKey da foto do profissional obtida via POST /api/v1/media/upload (scope EMPLOYEE_AVATAR)", nullable = true)
@Size(max = 1000, message = "O URL do avatar não pode exceder 1000 caracteres")
String avatarUrl) {

    @JsonCreator
    public CreateEmployeeRequest{}

    public CreateEmployeeRequest(UUID userId, String displayName, String specialty) {
        this(userId, displayName, specialty, null);
    }
}
