package cv.perola.modules.business.service;

import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Serviço público de consulta de salões, serviços e colaboradores do módulo Business para outros módulos.
 */
public interface BusinessEmployeeQueryService {

    Optional<BusinessResponse> findBusinessById(UUID businessId);

    Optional<EmployeeResponse> findEmployeeById(UUID employeeId);

    Optional<EmployeeResponse> findEmployeeByIdAndBusinessId(UUID employeeId, UUID businessId);

    Optional<ServiceResponse> findServiceByIdAndBusinessId(UUID serviceId, UUID businessId);

    boolean hasAnyAssignedServices(UUID businessId, UUID employeeId);

    boolean isServiceAssignedToEmployee(UUID businessId, UUID employeeId, UUID serviceId);
}
