package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.validator.business.CreateBusinessValidator;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateBusinessUseCase {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountService userAccountService;
    private final BusinessMapper businessMapper;
    private final EmployeeMapper employeeMapper;
    private final CreateBusinessValidator createBusinessValidator;

    @Transactional
    public BusinessResponse execute(UUID currentUserId, CreateBusinessRequest request) {
        // 1. Validação isolada de elegibilidade e regras de negócio
        createBusinessValidator.validate(currentUserId, request);

        // 2. Obter nome do utilizador no módulo Identity
        String ownerName = userAccountService.findUserNameById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        // 3. Salvar o novo Business
        BusinessEntity business = businessMapper.toEntity(request, currentUserId);
        BusinessEntity savedBusiness = businessRepository.save(business);

        // 4. Concessão de papéis no módulo de identidade
        userAccountService.assignRole(currentUserId, UserRole.OWNER);
        userAccountService.assignRole(currentUserId, UserRole.EMPLOYEE);

        // 5. Criação automática do perfil de funcionário inicial para o proprietário
        EmployeeEntity initialEmployee = employeeMapper.toEntity(
                savedBusiness.getId(),
                currentUserId,
                ownerName);
        employeeRepository.save(initialEmployee);

        return businessMapper.toResponse(savedBusiness);
    }
}
