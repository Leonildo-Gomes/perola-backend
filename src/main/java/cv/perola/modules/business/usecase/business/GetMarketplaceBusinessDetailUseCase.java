package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessDetailResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
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
public class GetMarketplaceBusinessDetailUseCase {

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository businessCategoryRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceAssignmentLookupService serviceAssignmentLookupService;
    private final BusinessMapper businessMapper;
    private final CategoryMapper categoryMapper;
    private final ServiceMapper serviceMapper;
    private final EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public MarketplaceBusinessDetailResponse execute(UUID businessId) {
        BusinessEntity business = businessRepository.findByIdAndIsActiveTrue(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Salão não encontrado ou inativo: " + businessId));

        List<BusinessCategoryEntity> categoryEntities = businessCategoryRepository.findAllByBusinessId(businessId);
        List<BusinessCategoryResponse> categories = categoryMapper.toBusinessCategoryResponses(categoryEntities);

        List<ServiceEntity> activeServices = serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId);
        List<UUID> serviceIds = activeServices.stream().map(ServiceEntity::getId).toList();
        Map<UUID, List<UUID>> assignedByService = serviceAssignmentLookupService
                .findAssignedEmployeeIdsByService(businessId, serviceIds);

        List<ServiceResponse> services = activeServices.stream()
                .map(service -> serviceMapper.toResponse(
                        service,
                        assignedByService.getOrDefault(service.getId(), List.of())
                ))
                .toList();

        List<EmployeeResponse> employees = employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)
                .stream()
                .map(employeeMapper::toResponse)
                .toList();

        return businessMapper.toDetailResponse(business, categories, services, employees);
    }
}
