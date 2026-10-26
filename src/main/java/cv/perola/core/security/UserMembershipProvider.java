package cv.perola.core.security;

import java.util.List;
import java.util.UUID;

public interface UserMembershipProvider {
    List<TenantMembership> findMembershipsByUserId(UUID userId);
}
