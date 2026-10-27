package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetWeeklyScheduleUseCase {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleBreakRepository breakRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final ScheduleMapper scheduleMapper;

    public WeeklyScheduleResponse execute(UUID employeeId, UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);

        List<EmployeeScheduleEntity> schedules = scheduleRepository.findAllByEmployeeIdOrderByDayOfWeekAsc(employeeId);
        if (schedules.isEmpty()) {
            return scheduleMapper.toResponse(employeeId, Collections.emptyList());
        }

        List<UUID> scheduleIds = schedules.stream().map(EmployeeScheduleEntity::getId).toList();
        List<EmployeeScheduleBreakEntity> allBreaks = breakRepository.findAllByEmployeeScheduleIdIn(scheduleIds);

        Map<UUID, List<EmployeeScheduleBreakEntity>> breaksBySchedule = allBreaks.stream()
                .collect(Collectors.groupingBy(EmployeeScheduleBreakEntity::getEmployeeScheduleId));

        List<WeeklyScheduleItemResponse> itemResponses = schedules.stream()
                .map(s -> scheduleMapper.toResponse(
                        s,
                        breaksBySchedule.getOrDefault(s.getId(), Collections.emptyList())
                ))
                .sorted(Comparator.comparing(WeeklyScheduleItemResponse::dayOfWeek))
                .toList();

        return scheduleMapper.toResponse(employeeId, itemResponses);
    }

    private void validatePermissions(EmployeeResponse employee, UserPrincipal principal, UUID businessId) {
        boolean isOwner = principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));

        if (isOwner) {
            return;
        }

        boolean isOwnEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isOwnEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode aceder ao seu próprio expediente.");
        }
    }
}
