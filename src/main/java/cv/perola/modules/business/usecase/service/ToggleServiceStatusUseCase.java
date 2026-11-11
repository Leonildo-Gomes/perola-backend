package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.ServiceAssignmentLookupService;
import cv.perola.modules.business.validator.service.ServiceStatusValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToggleServiceStatusUseCase {

    private final ServiceRepository serviceRepository;
    private final ServiceAssignmentLookupService serviceAssignmentLookupService;
    private final ServiceMapper serviceMapper;
    private final ServiceStatusValidator serviceStatusValidator;

    @Transactional
    public ServiceResponse execute(UUID businessId, UUID serviceId) {
        ServiceEntity service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + serviceId));

        boolean targetStatus = !service.isActive();

        if (targetStatus) {
            serviceStatusValidator.validateActivation(businessId, service);
        }

        service.setActive(targetStatus);
        ServiceEntity updated = serviceRepository.save(service);
        List<UUID> assignedEmployeeIds = serviceAssignmentLookupService.findAssignedEmployeeIds(businessId, serviceId);

        return serviceMapper.toResponse(updated, assignedEmployeeIds);
    }
}
