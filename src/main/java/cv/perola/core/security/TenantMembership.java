package cv.perola.core.security;

import java.util.UUID;

public record TenantMembership(
        UUID businessId,
        String role
) {}
