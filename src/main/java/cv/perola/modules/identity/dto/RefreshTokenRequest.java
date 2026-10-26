package cv.perola.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "O refreshToken é obrigatório")
        String refreshToken
) {}
