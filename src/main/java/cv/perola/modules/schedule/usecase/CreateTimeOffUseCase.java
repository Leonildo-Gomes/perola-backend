package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import cv.perola.modules.schedule.mapper.TimeOffMapper;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
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

    @Transactional
    public TimeOffResponse execute(UUID employeeId, CreateTimeOffRequest request, UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);
        validateDatesAndTimes(request);

        EmployeeTimeOffEntity entity = timeOffMapper.toEntity(employeeId, request);
        entity = timeOffRepository.save(entity);

        return timeOffMapper.toResponse(entity);
    }

    private void validatePermissions(EmployeeResponse employee, UserPrincipal principal, UUID businessId) {
        boolean isOwner = principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));

        if (isOwner) {
            return;
        }

        boolean isOwnEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isOwnEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode registar ausências para o seu próprio perfil.");
        }
    }

    private void validateDatesAndTimes(CreateTimeOffRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ValidationException("A data de término (" + request.endDate()
                    + ") não pode ser anterior à data de início (" + request.startDate() + ").");
        }

        if (!request.resolvedIsAllDay()) {
            if (request.startTime() == null || request.endTime() == null) {
                throw new ValidationException("Os horários de início e término são obrigatórios para ausências de período parcial.");
            }
            if (!request.endTime().isAfter(request.startTime())) {
                throw new ValidationException("O horário de término (" + request.endTime()
                        + ") deve ser posterior ao horário de início (" + request.startTime() + ").");
            }
        }
    }
}
