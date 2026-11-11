package cv.perola.modules.business.usecase.employee;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.service.BusinessEmployeeEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListActiveEmployeesUseCase {

    private final EmployeeRepository employeeRepository;
    private final BusinessEmployeeEnrichmentService businessEmployeeEnrichmentService;

    @Transactional(readOnly = true)
    public List<BusinessEmployeeResponse> execute(UUID businessId) {
        List<EmployeeEntity> employees = employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId);
        return businessEmployeeEnrichmentService.enrich(businessId, employees);
    }
}

