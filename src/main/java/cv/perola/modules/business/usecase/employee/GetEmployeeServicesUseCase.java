package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.EmployeeServicesResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetEmployeeServicesUseCase {

    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeServiceRepository employeeServiceRepository;
    private final ServiceMapper serviceMapper;

    @Transactional(readOnly = true)
    public EmployeeServicesResponse execute(UUID businessId, UUID employeeId) {
        EmployeeEntity employee = employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado ou inativo: " + employeeId));

        List<EmployeeServiceEntity> relations = employeeServiceRepository.findAllByBusinessIdAndEmployeeId(businessId, employeeId);
        if (relations.isEmpty()) {
            return new EmployeeServicesResponse(employeeId, businessId, Collections.emptyList());
        }

        List<UUID> serviceIds = relations.stream()
                .map(EmployeeServiceEntity::getServiceId)
                .toList();

        List<ServiceEntity> services = serviceRepository.findAllByBusinessIdAndIdIn(businessId, serviceIds);
        List<ServiceResponse> serviceResponses = services.stream()
                .map(serviceMapper::toResponse)
                .toList();

        return new EmployeeServicesResponse(employeeId, businessId, serviceResponses);
    }
}
