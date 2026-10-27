package cv.perola.modules.business.service;

import cv.perola.modules.business.dto.EmployeeResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Serviço público de consulta de colaboradores do módulo Business para outros módulos.
 */
public interface BusinessEmployeeQueryService {

    Optional<EmployeeResponse> findEmployeeById(UUID employeeId);

    Optional<EmployeeResponse> findEmployeeByIdAndBusinessId(UUID employeeId, UUID businessId);
}
