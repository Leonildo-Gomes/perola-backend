package cv.perola.modules.schedule.usecase.schedule;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.ScheduleDayItemResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
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
public class GetEmployeeScheduleUseCase {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleDayRepository scheduleDayRepository;
    private final EmployeeScheduleBreakRepository breakRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final ScheduleMapper scheduleMapper;

    public EmployeeScheduleResponse execute(UUID employeeId, UUID scheduleId, UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);

        EmployeeScheduleEntity schedule = scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de horário semanal não encontrado."));

        List<EmployeeScheduleDayEntity> days = scheduleDayRepository
                .findAllByEmployeeScheduleIdOrderByDayOfWeekAsc(schedule.getId());

        List<UUID> dayIds = days.stream().map(EmployeeScheduleDayEntity::getId).toList();
        List<EmployeeScheduleBreakEntity> breaks = dayIds.isEmpty()
                ? Collections.emptyList()
                : breakRepository.findAllByEmployeeScheduleDayIdIn(dayIds);

        Map<UUID, List<EmployeeScheduleBreakEntity>> breaksByDayId = breaks.stream()
                .collect(Collectors.groupingBy(EmployeeScheduleBreakEntity::getEmployeeScheduleDayId));

        List<ScheduleDayItemResponse> dayResponses = days.stream()
                .map(day -> scheduleMapper.toResponse(
                        day,
                        breaksByDayId.getOrDefault(day.getId(), Collections.emptyList())
                ))
                .sorted(Comparator.comparing(ScheduleDayItemResponse::dayOfWeek))
                .toList();

        return scheduleMapper.toResponse(schedule, dayResponses);
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

