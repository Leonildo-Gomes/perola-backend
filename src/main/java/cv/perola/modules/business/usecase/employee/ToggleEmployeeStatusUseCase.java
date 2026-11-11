package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.service.BusinessEmployeeEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToggleEmployeeStatusUseCase {

    private final EmployeeRepository employeeRepository;
    private final BusinessEmployeeEnrichmentService businessEmployeeEnrichmentService;

    @Transactional
    public BusinessEmployeeResponse execute(UUID businessId, UUID employeeId) {
        EmployeeEntity employee = employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado: " + employeeId));

        employee.setActive(!employee.isActive());
        EmployeeEntity updated = employeeRepository.save(employee);

        return businessEmployeeEnrichmentService.enrichSingle(businessId, updated);
    }
}
