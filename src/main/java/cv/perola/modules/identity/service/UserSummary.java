package cv.perola.modules.identity.service;

import java.util.UUID;

/**
 * Projeção pública e leve dos dados de contacto/perfil de um utilizador para consumo entre módulos.
 */
public record UserSummary(
        UUID id,
        String email,
        String phone,
        String avatarUrl
) {
}

