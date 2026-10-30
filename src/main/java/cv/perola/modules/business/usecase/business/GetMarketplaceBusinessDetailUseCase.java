package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessDetailResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMarketplaceBusinessDetailUseCase {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeRepository employeeRepository;
    private final BusinessMapper businessMapper;
    private final ServiceMapper serviceMapper;
    private final EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public MarketplaceBusinessDetailResponse execute(UUID businessId) {
        BusinessEntity business = businessRepository.findByIdAndIsActiveTrue(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Salão não encontrado ou inativo: " + businessId));

        List<ServiceResponse> services = serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId)
                .stream()
                .map(serviceMapper::toResponse)
                .toList();

        List<EmployeeResponse> employees = employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)
                .stream()
                .map(employeeMapper::toResponse)
                .toList();

        return businessMapper.toDetailResponse(business, services, employees);
    }
}
