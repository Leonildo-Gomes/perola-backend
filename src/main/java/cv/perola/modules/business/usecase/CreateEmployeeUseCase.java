package cv.perola.modules.business.usecase;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.validator.employee.CreateEmployeeValidator;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateEmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final UserAccountService userAccountService;
    private final EmployeeMapper employeeMapper;
    private final CreateEmployeeValidator createEmployeeValidator;

    @Transactional
    public EmployeeResponse execute(UUID businessId, CreateEmployeeRequest request) {
        createEmployeeValidator.validate(businessId, request);

        UUID userId = request.userId();
        if (userId != null) {
            userAccountService.assignRole(userId, UserRole.EMPLOYEE);
        }

        EmployeeEntity employee = employeeMapper.toEntity(request, businessId);
        EmployeeEntity saved = employeeRepository.save(employee);

        return employeeMapper.toResponse(saved);
    }
}
