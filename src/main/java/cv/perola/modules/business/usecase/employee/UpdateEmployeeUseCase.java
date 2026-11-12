package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.dto.UpdateEmployeeRequest;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.service.BusinessEmployeeEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateEmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final BusinessEmployeeEnrichmentService businessEmployeeEnrichmentService;

    @Transactional
    public BusinessEmployeeResponse execute(UUID businessId, UUID employeeId, UpdateEmployeeRequest request) {
        EmployeeEntity employee = employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado: " + employeeId));

        if (request.displayName() != null) {
            employee.setDisplayName(request.displayName().trim());
        }

        if (request.specialty() != null) {
            employee.setSpecialty(employeeMapper.normalizeSpecialty(request.specialty()));
        }

        if (request.avatarUrl() != null) {
            employee.setAvatarUrl(employeeMapper.normalizeUrl(request.avatarUrl()));
        }

        if (request.isActive() != null) {
            employee.setActive(request.isActive());
        }

        EmployeeEntity updated = employeeRepository.save(employee);
        return businessEmployeeEnrichmentService.enrichSingle(businessId, updated);
    }
}
