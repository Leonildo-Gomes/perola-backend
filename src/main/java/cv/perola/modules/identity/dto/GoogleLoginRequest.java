package cv.perola.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "O idToken é obrigatório")
        String idToken
) {}
