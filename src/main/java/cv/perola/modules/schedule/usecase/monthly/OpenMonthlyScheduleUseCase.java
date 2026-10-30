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
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.HolidayDecisionRequest;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import cv.perola.modules.schedule.mapper.MonthlyScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeMonthlyScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import cv.perola.modules.schedule.validator.monthly.OpenMonthlyScheduleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpenMonthlyScheduleUseCase {

    private final EmployeeMonthlyScheduleRepository monthlyScheduleRepository;
    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeTimeOffRepository timeOffRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final MonthlyScheduleMapper monthlyScheduleMapper;
    private final OpenMonthlyScheduleValidator openMonthlyScheduleValidator;

    @Transactional
    public MonthlyScheduleResponse execute(UUID employeeId, OpenMonthlyScheduleRequest request, UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        openMonthlyScheduleValidator.validatePermissions(employee, principal, businessId);

        EmployeeScheduleEntity schedule = scheduleRepository.findByIdAndEmployeeId(request.scheduleId(), employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de horário semanal não encontrado para este colaborador."));

        openMonthlyScheduleValidator.validateScheduleActive(schedule);
        openMonthlyScheduleValidator.validateHolidayDecisions(request.year(), request.month(), request.holidayDecisions());

        YearMonth yearMonth = YearMonth.of(request.year(), request.month());
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        timeOffRepository.deleteAllByEmployeeIdAndTypeAndStartDateBetween(
                employeeId, TimeOffType.HOLIDAY, monthStart, monthEnd
        );

        if (request.holidayDecisions() != null) {
            for (HolidayDecisionRequest decision : request.holidayDecisions()) {
                if (Boolean.FALSE.equals(decision.workOnHoliday())) {
                    EmployeeTimeOffEntity holidayTimeOff = EmployeeTimeOffEntity.builder()
                            .employeeId(employeeId)
                            .startDate(decision.holidayDate())
                            .endDate(decision.holidayDate())
                            .isAllDay(true)
                            .type(TimeOffType.HOLIDAY)
                            .note("Folga de feriado configurada na abertura mensal")
                            .build();
                    timeOffRepository.save(holidayTimeOff);
                }
            }
        }

        Instant now = Instant.now();
        Optional<EmployeeMonthlyScheduleEntity> existingMonthly = monthlyScheduleRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, request.year(), request.month());

        EmployeeMonthlyScheduleEntity monthlyEntity;
        if (existingMonthly.isPresent()) {
            monthlyEntity = existingMonthly.get();
            monthlyEntity.setEmployeeScheduleId(schedule.getId());
            monthlyEntity.setOpened(true);
            monthlyEntity.setOpenedAt(now);
        } else {
            monthlyEntity = monthlyScheduleMapper.toEntity(employeeId, request, now);
        }

        monthlyEntity = monthlyScheduleRepository.save(monthlyEntity);

        return monthlyScheduleMapper.toResponse(monthlyEntity, schedule.getName());
    }
}

