package cv.perola.modules.business.service;

import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserMembershipProvider;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.identity.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component("businessUserMembershipProvider")
@RequiredArgsConstructor
public class BusinessUserMembershipProvider implements UserMembershipProvider {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TenantMembership> findMembershipsByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<TenantMembership> memberships = new ArrayList<>();

        // 1. Verificar salão de que é proprietário (OWNER)
        businessRepository.findByOwnerId(userId)
                .ifPresent(business -> memberships.add(new TenantMembership(business.getId(), UserRole.OWNER.name())));

        // 2. Verificar salões onde é colaborador ativo (EMPLOYEE)
        employeeRepository.findByUserId(userId)
                .ifPresent(employee -> {
                    boolean alreadyPresent = memberships.stream()
                            .anyMatch(m -> m.businessId().equals(employee.getBusinessId()));
                    if (!alreadyPresent) {
                        memberships.add(new TenantMembership(employee.getBusinessId(), UserRole.EMPLOYEE.name()));
                    }
                });

        return Collections.unmodifiableList(memberships);
    }
}
