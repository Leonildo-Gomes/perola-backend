package cv.perola.modules.schedule.usecase.schedule;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import cv.perola.modules.schedule.validator.schedule.EmployeeScheduleValidator;
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
public class CreateEmployeeScheduleUseCase {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleDayRepository scheduleDayRepository;
    private final EmployeeScheduleBreakRepository breakRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final ScheduleMapper scheduleMapper;
    private final EmployeeScheduleValidator scheduleValidator;

    @Transactional
    public EmployeeScheduleResponse execute(UUID employeeId, SaveEmployeeScheduleRequest request,
            UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        scheduleValidator.validatePermissions(employee, principal, businessId);
        scheduleValidator.validateScheduleDaysAndBreaks(request.days());

        Optional<EmployeeScheduleEntity> currentDefault = scheduleRepository
                .findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(employeeId);

        boolean isActive = request.resolvedIsActive();
        boolean isDefault = isActive
                && (request.resolvedIsDefault() || (request.isDefault() == null && currentDefault.isEmpty()));

        if (isDefault && currentDefault.isPresent()) {
            EmployeeScheduleEntity prevDefault = currentDefault.get();
            prevDefault.setDefault(false);
            scheduleRepository.saveAndFlush(prevDefault);
        }

        EmployeeScheduleEntity scheduleEntity = scheduleMapper.toEntity(employeeId, request, isDefault);
        scheduleEntity = scheduleRepository.save(scheduleEntity);

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

    public static void validateScheduleDaysAndBreaks(List<ScheduleDayItemRequest> days) {
        new EmployeeScheduleValidator().validateScheduleDaysAndBreaks(days);
    }
}
