package cv.perola.modules.business.usecase;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
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
    private final BusinessRepository businessRepository;
    private final UserAccountService userAccountService;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public EmployeeResponse execute(UUID businessId, CreateEmployeeRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Salão não encontrado: " + businessId);
        }

        UUID userId = request.userId();
        if (userId != null) {
            if (!userAccountService.existsById(userId)) {
                throw new ResourceNotFoundException("Utilizador não encontrado: " + userId);
            }
            if (employeeRepository.existsByUserId(userId)) {
                throw new ConflictException("O utilizador indicado já está registado como profissional.");
            }
            userAccountService.assignRole(userId, UserRole.EMPLOYEE);
        }

        EmployeeEntity employee = employeeMapper.toEntity(request, businessId);
        EmployeeEntity saved = employeeRepository.save(employee);

        return employeeMapper.toResponse(saved);
    }
}
