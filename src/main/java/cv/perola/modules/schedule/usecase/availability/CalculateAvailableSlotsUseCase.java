package cv.perola.modules.schedule.usecase.availability;

import cv.perola.core.config.AppConfig;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.AvailabilitySlotsResponse;
import cv.perola.modules.schedule.mapper.AvailabilityMapper;
import cv.perola.modules.schedule.repository.EmployeeMonthlyScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import cv.perola.modules.schedule.service.BookedIntervalProvider;
import cv.perola.modules.schedule.service.LocalTimeWindow;
import cv.perola.modules.schedule.service.SlotCalculationEngine;
import cv.perola.modules.schedule.service.SlotCalculationInput;
import cv.perola.modules.schedule.service.TimeInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso para cálculo de slots disponíveis de um profissional para um serviço e data (BE-012).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculateAvailableSlotsUseCase {

    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final EmployeeMonthlyScheduleRepository monthlyScheduleRepository;
    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleDayRepository scheduleDayRepository;
    private final EmployeeScheduleBreakRepository scheduleBreakRepository;
    private final EmployeeTimeOffRepository timeOffRepository;
    private final BookedIntervalProvider bookedIntervalProvider;
    private final SlotCalculationEngine slotCalculationEngine;
    private final AvailabilityMapper availabilityMapper;
    private final Clock clock;

    public AvailabilitySlotsResponse execute(UUID businessId, UUID employeeId, UUID serviceId, LocalDate date) {
        if (employeeId == null) {
            throw new ValidationException("O identificador do profissional (employeeId) é obrigatório.");
        }
        if (serviceId == null) {
            throw new ValidationException("O identificador do serviço (serviceId) é obrigatório.");
        }
        if (date == null) {
            throw new ValidationException("A data de consulta (date) é obrigatória.");
        }

        EmployeeResponse employee = (businessId != null
                ? businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)
                : businessEmployeeQueryService.findEmployeeById(employeeId))
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));

        UUID resolvedBusinessId = employee.businessId();
        BusinessResponse business = businessEmployeeQueryService.findBusinessById(resolvedBusinessId)
                .orElseThrow(() -> new ResourceNotFoundException("Salão não encontrado."));

        ServiceResponse service = businessEmployeeQueryService
                .findServiceByIdAndBusinessId(serviceId, resolvedBusinessId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado para o salão especificado."));

        ZoneId businessZone = resolveZoneId(business.timezone());
        int durationMinutes = service.durationMinutes();

        if (!business.isActive() || !employee.isActive() || !service.isActive()) {
            return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, List.of(), businessZone);
        }

        if (businessEmployeeQueryService.hasAnyAssignedServices(resolvedBusinessId, employeeId)
                && !businessEmployeeQueryService.isServiceAssignedToEmployee(resolvedBusinessId, employeeId, serviceId)) {
            return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, List.of(), businessZone);
        }

        Optional<EmployeeMonthlyScheduleEntity> monthlyScheduleOpt = monthlyScheduleRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, date.getYear(), date.getMonthValue());
        if (monthlyScheduleOpt.isEmpty() || !monthlyScheduleOpt.get().isOpened()) {
            return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, List.of(), businessZone);
        }

        UUID scheduleId = monthlyScheduleOpt.get().getEmployeeScheduleId();
        Optional<EmployeeScheduleEntity> scheduleOpt = scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId);
        if (scheduleOpt.isEmpty() || !scheduleOpt.get().isActive()) {
            return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, List.of(), businessZone);
        }

        DayOfWeek domainDayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());
        Optional<EmployeeScheduleDayEntity> scheduleDayOpt = scheduleDayRepository
                .findByEmployeeScheduleIdAndDayOfWeek(scheduleId, domainDayOfWeek);
        if (scheduleDayOpt.isEmpty() || !scheduleDayOpt.get().isWorking()) {
            return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, List.of(), businessZone);
        }

        EmployeeScheduleDayEntity scheduleDay = scheduleDayOpt.get();

        List<EmployeeTimeOffEntity> timeOffs = timeOffRepository
                .findAllByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employeeId, date, date);
        boolean allDayTimeOff = timeOffs.stream().anyMatch(EmployeeTimeOffEntity::isAllDay);
        if (allDayTimeOff) {
            return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, List.of(), businessZone);
        }

        List<LocalTimeWindow> partialTimeOffs = timeOffs.stream()
                .filter(t -> !t.isAllDay() && t.getStartTime() != null && t.getEndTime() != null)
                .map(t -> new LocalTimeWindow(t.getStartTime(), t.getEndTime()))
                .toList();

        List<LocalTimeWindow> breaks = scheduleBreakRepository
                .findAllByEmployeeScheduleDayId(scheduleDay.getId())
                .stream()
                .map(b -> new LocalTimeWindow(b.getStartTime(), b.getEndTime()))
                .toList();

        Instant dayStart = date.atStartOfDay(businessZone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(businessZone).toInstant();
        List<TimeInterval> bookedIntervals = bookedIntervalProvider.findBookedIntervals(employeeId, dayStart, dayEnd);

        SlotCalculationInput input = new SlotCalculationInput(
                date,
                businessZone,
                durationMinutes,
                true,
                scheduleDay.isWorking(),
                scheduleDay.getStartTime(),
                scheduleDay.getEndTime(),
                breaks,
                false,
                partialTimeOffs,
                bookedIntervals,
                Instant.now(clock)
        );

        List<TimeInterval> availableIntervals = slotCalculationEngine.calculateSlots(input);
        return availabilityMapper.toResponse(date, employeeId, serviceId, durationMinutes, availableIntervals, businessZone);
    }

    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ignored) {
            return ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        }
    }
}

