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
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateEmployeeScheduleUseCase {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleDayRepository scheduleDayRepository;
    private final EmployeeScheduleBreakRepository breakRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final ScheduleMapper scheduleMapper;

    @Transactional
    public EmployeeScheduleResponse execute(
            UUID employeeId,
            UUID scheduleId,
            SaveEmployeeScheduleRequest request,
            UserPrincipal principal
    ) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);

        EmployeeScheduleEntity scheduleEntity = scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de horário semanal não encontrado."));

        CreateEmployeeScheduleUseCase.validateScheduleDaysAndBreaks(request.days());

        boolean isActive = request.resolvedIsActive();
        boolean isDefault = isActive && request.resolvedIsDefault();

        if (isDefault) {
            Optional<EmployeeScheduleEntity> currentDefault = scheduleRepository
                    .findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(employeeId);
            if (currentDefault.isPresent() && !currentDefault.get().getId().equals(scheduleId)) {
                EmployeeScheduleEntity prevDefault = currentDefault.get();
                prevDefault.setDefault(false);
                scheduleRepository.saveAndFlush(prevDefault);
            }
        }

        scheduleEntity.setName(request.name().trim());
        scheduleEntity.setActive(isActive);
        scheduleEntity.setDefault(isDefault);
        scheduleEntity = scheduleRepository.saveAndFlush(scheduleEntity);

        List<EmployeeScheduleDayEntity> existingDays = scheduleDayRepository
                .findAllByEmployeeScheduleIdOrderByDayOfWeekAsc(scheduleId);
        if (!existingDays.isEmpty()) {
            List<UUID> existingDayIds = existingDays.stream().map(EmployeeScheduleDayEntity::getId).toList();
            breakRepository.deleteAllByEmployeeScheduleDayIdIn(existingDayIds);
            scheduleDayRepository.deleteAllByEmployeeScheduleId(scheduleId);
            scheduleDayRepository.flush();
        }

        List<ScheduleDayItemResponse> dayResponses = new ArrayList<>();

        for (ScheduleDayItemRequest dayReq : request.days()) {
            EmployeeScheduleDayEntity dayEntity = scheduleMapper.toEntity(scheduleEntity.getId(), dayReq);
            dayEntity = scheduleDayRepository.save(dayEntity);

            List<EmployeeScheduleBreakEntity> savedBreaks = new ArrayList<>();
            if (dayEntity.isWorking() && dayReq.breaks() != null && !dayReq.breaks().isEmpty()) {
                for (BreakItemRequest breakReq : dayReq.breaks()) {
                    EmployeeScheduleBreakEntity breakEntity = scheduleMapper.toEntity(dayEntity.getId(), breakReq);
                    savedBreaks.add(breakRepository.save(breakEntity));
                }
            }

            dayResponses.add(scheduleMapper.toResponse(dayEntity, savedBreaks));
        }

        dayResponses.sort(Comparator.comparing(ScheduleDayItemResponse::dayOfWeek));

        return scheduleMapper.toResponse(scheduleEntity, dayResponses);
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
}

