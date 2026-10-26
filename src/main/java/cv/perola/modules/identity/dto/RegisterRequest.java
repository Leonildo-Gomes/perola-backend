package cv.perola.modules.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres")
        String name,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O formato do email é inválido")
        @Size(max = 255, message = "O email não pode exceder 255 caracteres")
        String email,

        @NotBlank(message = "O telefone é obrigatório")
        @Size(max = 50, message = "O telefone não pode exceder 50 caracteres")
        String phone,

        @NotBlank(message = "A palavra-passe é obrigatória")
        @Size(min = 8, message = "A palavra-passe deve conter pelo menos 8 caracteres")
        String password
) {}
