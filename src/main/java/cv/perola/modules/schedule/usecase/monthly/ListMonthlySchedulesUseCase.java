package cv.perola.modules.schedule.usecase.monthly;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.mapper.MonthlyScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeMonthlyScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMonthlySchedulesUseCase {

    private final EmployeeMonthlyScheduleRepository monthlyScheduleRepository;
    private final EmployeeScheduleRepository scheduleRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final MonthlyScheduleMapper monthlyScheduleMapper;

    public List<MonthlyScheduleResponse> execute(UUID employeeId, int year, UserPrincipal principal) {
        if (year < 2024 || year > 2100) {
            throw new ValidationException("O ano deve estar compreendido entre 2024 e 2100.");
        }

        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);

        List<EmployeeMonthlyScheduleEntity> monthlyEntities = monthlyScheduleRepository
                .findAllByEmployeeIdAndYearOrderByMonthAsc(employeeId, year);

        if (monthlyEntities.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> scheduleIds = monthlyEntities.stream()
                .map(EmployeeMonthlyScheduleEntity::getEmployeeScheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> scheduleNames = scheduleRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(
                        EmployeeScheduleEntity::getId,
                        EmployeeScheduleEntity::getName,
                        (existing, replacement) -> existing
                ));

        return monthlyEntities.stream()
                .map(entity -> monthlyScheduleMapper.toResponse(
                        entity,
                        scheduleNames.getOrDefault(entity.getEmployeeScheduleId(), "Horário Padrão")
                ))
                .toList();
    }

    private void validatePermissions(EmployeeResponse employee, UserPrincipal principal, UUID businessId) {
        boolean isOwner = principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));

        if (isOwner) {
            return;
        }

        boolean isOwnEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isOwnEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode consultar a sua própria agenda mensal.");
        }
    }
}

