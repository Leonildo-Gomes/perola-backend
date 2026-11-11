package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.AssignEmployeeServicesRequest;
import cv.perola.modules.business.dto.EmployeeServicesResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.ServiceAssignmentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignEmployeeServicesUseCase {

    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeServiceRepository employeeServiceRepository;
    private final ServiceAssignmentLookupService serviceAssignmentLookupService;
    private final ServiceMapper serviceMapper;

    @Transactional
    public EmployeeServicesResponse execute(UUID businessId, UUID employeeId, AssignEmployeeServicesRequest request) {
        EmployeeEntity employee = employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado ou inativo: " + employeeId));

        List<UUID> requestedServiceIds = request.serviceIds();
        List<ServiceEntity> validServices = new ArrayList<>();

        if (requestedServiceIds != null && !requestedServiceIds.isEmpty()) {
            validServices = serviceRepository.findAllByBusinessIdAndIdIn(businessId, requestedServiceIds);
            if (validServices.size() != requestedServiceIds.size()) {
                throw new ResourceNotFoundException("Um ou mais serviços indicados não existem ou não pertencem ao salão.");
            }
        }

        // Limpar atribuições anteriores do colaborador neste salão e sincronizar antes de inserir
        employeeServiceRepository.deleteAllByBusinessIdAndEmployeeId(businessId, employeeId);
        employeeServiceRepository.flush();

        // Atribuir novos serviços
        if (!validServices.isEmpty()) {
            List<EmployeeServiceEntity> relations = validServices.stream()
                    .map(service -> EmployeeServiceEntity.builder()
                            .businessId(businessId)
                            .employeeId(employee.getId())
                            .serviceId(service.getId())
                            .build())
                    .toList();
            employeeServiceRepository.saveAll(relations);
            employeeServiceRepository.flush();
        }

        List<UUID> serviceIds = validServices.stream().map(ServiceEntity::getId).toList();
        Map<UUID, List<UUID>> assignedByService = serviceAssignmentLookupService
                .findAssignedEmployeeIdsByService(businessId, serviceIds);

        List<ServiceResponse> serviceResponses = validServices.stream()
                .map(service -> serviceMapper.toResponse(
                        service,
                        assignedByService.getOrDefault(service.getId(), List.of())
                ))
                .toList();

        return new EmployeeServicesResponse(employeeId, businessId, serviceResponses);
    }
}
