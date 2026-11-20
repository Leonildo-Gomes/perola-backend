package cv.perola.modules.identity.service;

import java.util.UUID;

/**
 * Projeção leve de cliente para comunicação inter-módulos desacoplada.
 */
public record CustomerSummary(
        UUID id,
        UUID userId,
        String name,
        String email,
        String phone
) {
    public boolean hasRegisteredAccount() {
        return userId != null;
    }
}
