package cv.perola.modules.business.validator.employee;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.identity.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateEmployeeValidator {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountService userAccountService;

    public void validate(UUID businessId, CreateEmployeeRequest request) {
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
        }
    }
}

