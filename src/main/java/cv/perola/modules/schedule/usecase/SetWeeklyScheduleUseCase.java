package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.SetWeeklyScheduleRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetWeeklyScheduleUseCase {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleBreakRepository breakRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final ScheduleMapper scheduleMapper;

    @Transactional
    public WeeklyScheduleResponse execute(UUID employeeId, SetWeeklyScheduleRequest request, UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);
        validateSchedulesAndBreaks(request.schedules());

        List<EmployeeScheduleEntity> existingSchedules = scheduleRepository.findAllByEmployeeId(employeeId);
        if (!existingSchedules.isEmpty()) {
            List<UUID> existingScheduleIds = existingSchedules.stream().map(EmployeeScheduleEntity::getId).toList();
            breakRepository.deleteAllByEmployeeScheduleIdIn(existingScheduleIds);
            scheduleRepository.deleteAllByEmployeeId(employeeId);
            scheduleRepository.flush();
        }

        List<WeeklyScheduleItemResponse> itemResponses = new ArrayList<>();

        for (WeeklyScheduleItemRequest itemReq : request.schedules()) {
            EmployeeScheduleEntity scheduleEntity = scheduleMapper.toEntity(employeeId, itemReq);
            scheduleEntity = scheduleRepository.save(scheduleEntity);

            List<EmployeeScheduleBreakEntity> savedBreaks = new ArrayList<>();
            if (itemReq.breaks() != null && !itemReq.breaks().isEmpty()) {
                for (BreakItemRequest breakReq : itemReq.breaks()) {
                    EmployeeScheduleBreakEntity breakEntity = scheduleMapper.toEntity(scheduleEntity.getId(), breakReq);
                    savedBreaks.add(breakRepository.save(breakEntity));
                }
            }

            itemResponses.add(scheduleMapper.toResponse(scheduleEntity, savedBreaks));
        }

        itemResponses.sort(Comparator.comparing(WeeklyScheduleItemResponse::dayOfWeek));

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
            throw new TenantAccessDeniedException("O colaborador só pode gerir o seu próprio expediente.");
        }
    }

    private void validateSchedulesAndBreaks(List<WeeklyScheduleItemRequest> schedules) {
        Set<DayOfWeek> days = new HashSet<>();

        for (WeeklyScheduleItemRequest schedule : schedules) {
            if (!days.add(schedule.dayOfWeek())) {
                throw new ValidationException("O dia da semana " + schedule.dayOfWeek() + " foi configurado mais de uma vez.");
            }

            if (!schedule.endTime().isAfter(schedule.startTime())) {
                throw new ValidationException("O horário de término (" + schedule.endTime()
                        + ") deve ser posterior ao horário de início (" + schedule.startTime() + ") para " + schedule.dayOfWeek() + ".");
            }

            if (schedule.breaks() != null && !schedule.breaks().isEmpty()) {
                List<BreakItemRequest> sortedBreaks = new ArrayList<>(schedule.breaks());
                sortedBreaks.sort(Comparator.comparing(BreakItemRequest::startTime));

                for (BreakItemRequest b : sortedBreaks) {
                    if (!b.endTime().isAfter(b.startTime())) {
                        throw new ValidationException("O horário de término da pausa '" + b.title()
                                + "' deve ser posterior ao horário de início.");
                    }

                    if (b.startTime().isBefore(schedule.startTime()) || b.endTime().isAfter(schedule.endTime())) {
                        throw new ValidationException("A pausa '" + b.title() + "' (" + b.startTime() + " - " + b.endTime()
                                + ") deve estar totalmente contida no turno (" + schedule.startTime() + " - " + schedule.endTime() + ").");
                    }
                }

                for (int i = 0; i < sortedBreaks.size() - 1; i++) {
                    BreakItemRequest current = sortedBreaks.get(i);
                    BreakItemRequest next = sortedBreaks.get(i + 1);

                    if (current.endTime().isAfter(next.startTime())) {
                        throw new ValidationException("As pausas '" + current.title() + "' e '" + next.title()
                                + "' sobrepõem-se no dia " + schedule.dayOfWeek() + ".");
                    }
                }
            }
        }
    }
}
