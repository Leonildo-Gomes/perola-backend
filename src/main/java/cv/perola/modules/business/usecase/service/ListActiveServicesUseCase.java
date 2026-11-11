package cv.perola.modules.business.usecase.service;

import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.ServiceAssignmentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListActiveServicesUseCase {

        private final ServiceRepository serviceRepository;
        private final ServiceAssignmentLookupService serviceAssignmentLookupService;
        private final ServiceMapper serviceMapper;

        @Transactional(readOnly = true)
        public List<ServiceResponse> execute(UUID businessId) {
                List<ServiceEntity> services = serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId);
                List<UUID> serviceIds = services.stream().map(ServiceEntity::getId).toList();
                Map<UUID, List<UUID>> assignedByService = serviceAssignmentLookupService
                                .findAssignedEmployeeIdsByService(businessId, serviceIds);

                return services.stream()
                                .map(service -> serviceMapper.toResponse(
                                                service,
                                                assignedByService.getOrDefault(service.getId(), List.of())))
                                .toList();
        }
}