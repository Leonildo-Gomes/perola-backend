package cv.perola.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para atualização do perfil do utilizador autenticado")
public record UpdateUserProfileRequest(
        @Schema(description = "Nome do utilizador", example = "Carlos Silva")
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres")
        String name,

        @Schema(description = "Telefone de contacto", example = "+2389912345")
        @Size(max = 50, message = "O telefone não pode exceder 50 caracteres")
        String phone,

        @Schema(description = "URL ou storageKey do avatar obtido via POST /api/v1/media/upload (scope USER_AVATAR)", nullable = true)
        @Size(max = 1000, message = "O URL do avatar não pode exceder 1000 caracteres")
        String avatarUrl
) {
}

