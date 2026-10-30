package cv.perola.modules.schedule.usecase.schedule;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeScheduleUseCaseTest {

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleDayRepository scheduleDayRepository;

    @Mock
    private EmployeeScheduleBreakRepository breakRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    private UpdateEmployeeScheduleUseCase useCase;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(businessId);
        useCase = new UpdateEmployeeScheduleUseCase(
                scheduleRepository,
                scheduleDayRepository,
                breakRepository,
                employeeQueryService,
                new ScheduleMapper()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve atualizar perfil de horário substituindo dias e pausas de forma transacional")
    void shouldUpdateEmployeeScheduleAndReplaceDaysAndBreaks() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        EmployeeScheduleEntity existingSchedule = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Antigo")
                .isDefault(false)
                .isActive(true)
                .build();
        existingSchedule.setId(scheduleId);

        EmployeeScheduleDayEntity oldDay = EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(scheduleId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .isWorking(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        oldDay.setId(UUID.randomUUID());

        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário de Dezembro",
                true,
                true,
                CreateEmployeeScheduleUseCaseTest.buildValidSevenDays()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.of(existingSchedule));
        when(scheduleRepository.findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(employeeId)).thenReturn(Optional.empty());
        when(scheduleRepository.saveAndFlush(any(EmployeeScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleDayRepository.findAllByEmployeeScheduleIdOrderByDayOfWeekAsc(scheduleId)).thenReturn(List.of(oldDay));
        when(scheduleDayRepository.save(any(EmployeeScheduleDayEntity.class))).thenAnswer(inv -> {
            EmployeeScheduleDayEntity d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(breakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeScheduleResponse response = useCase.execute(employeeId, scheduleId, request, principal);

        assertThat(response.id()).isEqualTo(scheduleId);
        assertThat(response.name()).isEqualTo("Horário de Dezembro");
        assertThat(response.isDefault()).isTrue();
        assertThat(response.days()).hasSize(7);
        verify(breakRepository).deleteAllByEmployeeScheduleDayIdIn(List.of(oldDay.getId()));
        verify(scheduleDayRepository).deleteAllByEmployeeScheduleId(scheduleId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando perfil de horário não for encontrado")
    void shouldThrowResourceNotFoundExceptionWhenScheduleNotFound() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário de Dezembro", true, true, CreateEmployeeScheduleUseCaseTest.buildValidSevenDays()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(employeeId, scheduleId, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}