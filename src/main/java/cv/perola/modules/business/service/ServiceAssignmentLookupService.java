package cv.perola.modules.business.service;

import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository.ServiceEmployeeAssignmentProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAssignmentLookupService {

    private final EmployeeServiceRepository employeeServiceRepository;

    @Transactional(readOnly = true)
    public Map<UUID, List<UUID>> findAssignedEmployeeIdsByService(UUID businessId, Collection<UUID> serviceIds) {
        if (businessId == null || serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return employeeServiceRepository.findActiveAssignmentsByBusinessIdAndServiceIdIn(businessId, serviceIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ServiceEmployeeAssignmentProjection::getServiceId,
                        Collectors.mapping(ServiceEmployeeAssignmentProjection::getEmployeeId, Collectors.toList())
                ));
    }

    @Transactional(readOnly = true)
    public List<UUID> findAssignedEmployeeIds(UUID businessId, UUID serviceId) {
        if (businessId == null || serviceId == null) {
            return Collections.emptyList();
        }
        return findAssignedEmployeeIdsByService(businessId, List.of(serviceId))
                .getOrDefault(serviceId, Collections.emptyList());
    }
}

