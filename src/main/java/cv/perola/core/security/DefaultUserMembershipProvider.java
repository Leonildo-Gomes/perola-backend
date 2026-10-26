package cv.perola.core.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnMissingBean(name = "businessUserMembershipProvider")
public class DefaultUserMembershipProvider implements UserMembershipProvider {

    @Override
    public List<TenantMembership> findMembershipsByUserId(UUID userId) {
        return Collections.emptyList();
    }
}
