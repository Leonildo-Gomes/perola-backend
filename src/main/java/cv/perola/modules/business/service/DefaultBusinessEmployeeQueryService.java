package cv.perola.modules.business.service;

import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultBusinessEmployeeQueryService implements BusinessEmployeeQueryService {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeServiceRepository employeeServiceRepository;
    private final BusinessMapper businessMapper;
    private final EmployeeMapper employeeMapper;
    private final ServiceMapper serviceMapper;

    @Override
    public Optional<BusinessResponse> findBusinessById(UUID businessId) {
        return businessRepository.findById(businessId)
                .map(businessMapper::toResponse);
    }

    @Override
    public Optional<EmployeeResponse> findEmployeeById(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employeeMapper::toResponse);
    }

    @Override
    public Optional<EmployeeResponse> findEmployeeByIdAndBusinessId(UUID employeeId, UUID businessId) {
        return employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .map(employeeMapper::toResponse);
    }

    @Override
    public Optional<EmployeeResponse> findEmployeeByBusinessIdAndUserId(UUID businessId, UUID userId) {
        return employeeRepository.findByBusinessIdAndUserId(businessId, userId)
                .map(employeeMapper::toResponse);
    }

    @Override
    public Optional<ServiceResponse> findServiceByIdAndBusinessId(UUID serviceId, UUID businessId) {
        return serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .map(serviceMapper::toResponse);
    }

    @Override
    public boolean hasAnyAssignedServices(UUID businessId, UUID employeeId) {
        return !employeeServiceRepository.findAllByBusinessIdAndEmployeeId(businessId, employeeId).isEmpty();
    }

    @Override
    public boolean isServiceAssignedToEmployee(UUID businessId, UUID employeeId, UUID serviceId) {
        return employeeServiceRepository.existsByBusinessIdAndEmployeeIdAndServiceId(businessId, employeeId, serviceId);
    }
}
