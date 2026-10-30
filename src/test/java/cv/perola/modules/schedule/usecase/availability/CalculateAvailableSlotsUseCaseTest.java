package cv.perola.modules.schedule.usecase.availability;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.AvailabilitySlotsResponse;
import cv.perola.modules.schedule.mapper.AvailabilityMapper;
import cv.perola.modules.schedule.repository.EmployeeMonthlyScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import cv.perola.modules.schedule.service.BookedIntervalProvider;
import cv.perola.modules.schedule.service.SlotCalculationEngine;
import cv.perola.modules.schedule.service.SlotCalculationInput;
import cv.perola.modules.schedule.service.TimeInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateAvailableSlotsUseCaseTest {

    private static final ZoneId CAPE_VERDE_ZONE = ZoneId.of("Atlantic/Cape_Verde");
    private static final Instant FIXED_NOW = ZonedDateTime.of(
            LocalDate.of(2026, 10, 15), LocalTime.of(9, 0), CAPE_VERDE_ZONE
    ).toInstant();

    @Mock
    private BusinessEmployeeQueryService businessEmployeeQueryService;

    @Mock
    private EmployeeMonthlyScheduleRepository monthlyScheduleRepository;

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleDayRepository scheduleDayRepository;

    @Mock
    private EmployeeScheduleBreakRepository scheduleBreakRepository;

    @Mock
    private EmployeeTimeOffRepository timeOffRepository;

    @Mock
    private BookedIntervalProvider bookedIntervalProvider;

    @Mock
    private SlotCalculationEngine slotCalculationEngine;

    private AvailabilityMapper availabilityMapper;
    private Clock fixedClock;
    private CalculateAvailableSlotsUseCase useCase;

    private UUID businessId;
    private UUID employeeId;
    private UUID serviceId;
    private UUID scheduleId;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        availabilityMapper = new AvailabilityMapper();
        fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        useCase = new CalculateAvailableSlotsUseCase(
                businessEmployeeQueryService,
                monthlyScheduleRepository,
                scheduleRepository,
                scheduleDayRepository,
                scheduleBreakRepository,
                timeOffRepository,
                bookedIntervalProvider,
                slotCalculationEngine,
                availabilityMapper,
                fixedClock
        );

        businessId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        targetDate = LocalDate.of(2026, 11, 2); // Monday
    }

    @Test
    @DisplayName("Deve calcular e devolver os slots disponíveis quando o mês está aberto e o dia é trabalhado")
    void shouldCalculateAndReturnAvailableSlotsWhenMonthIsOpenedAndDayIsWorking() {
        // Arrange
        mockActiveBusinessEmployeeAndService();
        when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);

        EmployeeMonthlyScheduleEntity monthlySchedule = EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(scheduleId)
                .year(2026)
                .month(11)
                .isOpened(true)
                .build();
        when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 11))
                .thenReturn(Optional.of(monthlySchedule));

        EmployeeScheduleEntity scheduleProfile = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Padrão")
                .isDefault(true)
                .isActive(true)
                .build();
        when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId))
                .thenReturn(Optional.of(scheduleProfile));

        EmployeeScheduleDayEntity scheduleDay = EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(scheduleId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .isWorking(true)
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(16, 0))
                .build();
        when(scheduleDayRepository.findByEmployeeScheduleIdAndDayOfWeek(scheduleId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(scheduleDay));

        when(timeOffRepository.findAllByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId, targetDate, targetDate)).thenReturn(List.of());

        EmployeeScheduleBreakEntity lunchBreak = EmployeeScheduleBreakEntity.builder()
                .title("Almoço")
                .startTime(LocalTime.of(12, 30))
                .endTime(LocalTime.of(13, 0))
                .build();
        when(scheduleBreakRepository.findAllByEmployeeScheduleDayId(any()))
                .thenReturn(List.of(lunchBreak));

        when(bookedIntervalProvider.findBookedIntervals(any(), any(), any())).thenReturn(List.of());

        TimeInterval slot1 = new TimeInterval(
                ZonedDateTime.of(targetDate, LocalTime.of(8, 30), CAPE_VERDE_ZONE).toInstant(),
                ZonedDateTime.of(targetDate, LocalTime.of(9, 30), CAPE_VERDE_ZONE).toInstant()
        );
        when(slotCalculationEngine.calculateSlots(any(SlotCalculationInput.class)))
                .thenReturn(List.of(slot1));

        // Act
        AvailabilitySlotsResponse response = useCase.execute(businessId, employeeId, serviceId, targetDate);

        // Assert
        assertThat(response.date()).isEqualTo(targetDate);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.serviceId()).isEqualTo(serviceId);
        assertThat(response.durationMinutes()).isEqualTo(60);
        assertThat(response.slots()).hasSize(1);
        verify(slotCalculationEngine).calculateSlots(any(SlotCalculationInput.class));
    }

    @Test
    @DisplayName("Deve retornar 0 slots quando o mês ainda não foi aberto pelo profissional")
    void shouldReturnEmptySlotsWhenMonthIsNotOpened() {
        // Arrange
        mockActiveBusinessEmployeeAndService();
        when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);
        when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 11))
                .thenReturn(Optional.empty());

        // Act
        AvailabilitySlotsResponse response = useCase.execute(businessId, employeeId, serviceId, targetDate);

        // Assert
        assertThat(response.slots()).isEmpty();
        verifyNoInteractions(slotCalculationEngine);
    }

    @Test
    @DisplayName("Deve retornar 0 slots quando existe ausência ou feriado de dia inteiro na data")
    void shouldReturnEmptySlotsWhenAllDayTimeOffExistsOnDate() {
        // Arrange
        mockActiveBusinessEmployeeAndService();
        when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);

        EmployeeMonthlyScheduleEntity monthlySchedule = EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(scheduleId)
                .year(2026)
                .month(11)
                .isOpened(true)
                .build();
        when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 11))
                .thenReturn(Optional.of(monthlySchedule));

        EmployeeScheduleEntity scheduleProfile = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Padrão")
                .isActive(true)
                .build();
        when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId))
                .thenReturn(Optional.of(scheduleProfile));

        EmployeeScheduleDayEntity scheduleDay = EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(scheduleId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .isWorking(true)
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(16, 0))
                .build();
        when(scheduleDayRepository.findByEmployeeScheduleIdAndDayOfWeek(scheduleId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(scheduleDay));

        EmployeeTimeOffEntity holidayOff = EmployeeTimeOffEntity.builder()
                .employeeId(employeeId)
                .startDate(targetDate)
                .endDate(targetDate)
                .isAllDay(true)
                .type(TimeOffType.HOLIDAY)
                .build();
        when(timeOffRepository.findAllByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId, targetDate, targetDate)).thenReturn(List.of(holidayOff));

        // Act
        AvailabilitySlotsResponse response = useCase.execute(businessId, employeeId, serviceId, targetDate);

        // Assert
        assertThat(response.slots()).isEmpty();
        verifyNoInteractions(slotCalculationEngine);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o profissional não existe")
    void shouldThrowResourceNotFoundExceptionWhenEmployeeNotFound() {
        // Arrange
        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId, employeeId, serviceId, targetDate))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando parâmetros obrigatórios forem nulos")
    void shouldThrowValidationExceptionWhenRequiredParametersAreNull() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId, null, serviceId, targetDate))
                .isInstanceOf(ValidationException.class);
    }

    private void mockActiveBusinessEmployeeAndService() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Maria", true, Instant.now(), Instant.now()
        );
        BusinessResponse business = new BusinessResponse(
                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                "Platô", "+2389911223", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now()
        );
        ServiceResponse service = new ServiceResponse(
                serviceId, businessId, "Corte & Brushing", null, 60,
                new BigDecimal("2500.00"), true, Instant.now(), Instant.now()
        );

        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId))
                .thenReturn(Optional.of(employee));
        when(businessEmployeeQueryService.findBusinessById(businessId))
                .thenReturn(Optional.of(business));
        when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId))
                .thenReturn(Optional.of(service));
    }
}

