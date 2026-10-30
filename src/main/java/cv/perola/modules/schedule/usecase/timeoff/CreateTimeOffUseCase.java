package cv.perola.modules.schedule.usecase.timeoff;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import cv.perola.modules.schedule.mapper.TimeOffMapper;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import cv.perola.modules.schedule.validator.timeoff.CreateTimeOffValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateTimeOffUseCase {

    private final EmployeeTimeOffRepository timeOffRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final TimeOffMapper timeOffMapper;
    private final CreateTimeOffValidator timeOffValidator;

    @Transactional
    public TimeOffResponse execute(UUID employeeId, CreateTimeOffRequest request, UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        timeOffValidator.validatePermissions(employee, principal, businessId);
        timeOffValidator.validateDatesAndTimes(request);

        EmployeeTimeOffEntity entity = timeOffMapper.toEntity(employeeId, request);
        entity = timeOffRepository.save(entity);

        return timeOffMapper.toResponse(entity);
    }
}
