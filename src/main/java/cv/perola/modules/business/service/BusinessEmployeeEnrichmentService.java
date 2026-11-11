package cv.perola.modules.business.service;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.service.UserAccountService;
import cv.perola.modules.identity.service.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enriquece entidades de colaboradores com dados de utilizador, papel no salão e serviços atribuídos em lote (sem N+1).
 */
@Service
@RequiredArgsConstructor
public class BusinessEmployeeEnrichmentService {

    private final BusinessRepository businessRepository;
    private final ServiceAssignmentLookupService serviceAssignmentLookupService;
    private final UserAccountService userAccountService;
    private final EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public List<BusinessEmployeeResponse> enrich(UUID businessId, List<EmployeeEntity> employees) {
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }

        UUID ownerId = businessRepository.findOwnerIdById(businessId).orElse(null);

        List<UUID> employeeIds = employees.stream()
                .map(EmployeeEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, List<UUID>> servicesByEmployee = serviceAssignmentLookupService
                .findAssignedServiceIdsByEmployee(businessId, employeeIds);

        Set<UUID> userIds = employees.stream()
                .map(EmployeeEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserSummary> usersById = userAccountService.findUserSummariesByIds(userIds);

        return employees.stream()
                .map(employee -> {
                    UUID userId = employee.getUserId();
                    UserSummary userSummary = userId != null ? usersById.get(userId) : null;
                    String accessRole = resolveAccessRole(userId, ownerId);
                    List<UUID> assignedServices = servicesByEmployee.getOrDefault(employee.getId(), List.of());
                    return employeeMapper.toBusinessResponse(employee, userSummary, accessRole, assignedServices);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessEmployeeResponse enrichSingle(UUID businessId, EmployeeEntity employee) {
        if (employee == null) {
            return null;
        }
        return enrich(businessId, List.of(employee)).getFirst();
    }

    private String resolveAccessRole(UUID userId, UUID ownerId) {
        if (userId == null) {
            return null;
        }
        if (userId.equals(ownerId)) {
            return UserRole.OWNER.name();
        }
        return UserRole.EMPLOYEE.name();
    }
}

