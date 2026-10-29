package cv.perola.modules.schedule.usecase;

import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeeSchedulesUseCaseTest {

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleDayRepository scheduleDayRepository;

    @Mock
    private EmployeeScheduleBreakRepository breakRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    private ListEmployeeSchedulesUseCase useCase;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(businessId);
        useCase = new ListEmployeeSchedulesUseCase(
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
    @DisplayName("Deve listar todos os perfis de horário do colaborador com dias e pausas")
    void shouldListEmployeeSchedulesWithDaysAndBreaks() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        UUID scheduleId = UUID.randomUUID();
        UUID dayId = UUID.randomUUID();

        EmployeeScheduleEntity scheduleEntity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Padrão")
                .isDefault(true)
                .isActive(true)
                .build();
        scheduleEntity.setId(scheduleId);

        EmployeeScheduleDayEntity dayEntity = EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(scheduleId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .isWorking(true)
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(16, 0))
                .build();
        dayEntity.setId(dayId);

        EmployeeScheduleBreakEntity breakEntity = EmployeeScheduleBreakEntity.builder()
                .id(UUID.randomUUID())
                .employeeScheduleDayId(dayId)
                .title("Almoço")
                .startTime(LocalTime.of(12, 30))
                .endTime(LocalTime.of(13, 0))
                .build();

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findAllByEmployeeIdOrderByIsDefaultDescCreatedAtAsc(employeeId))
                .thenReturn(List.of(scheduleEntity));
        when(scheduleDayRepository.findAllByEmployeeScheduleIdIn(List.of(scheduleId)))
                .thenReturn(List.of(dayEntity));
        when(breakRepository.findAllByEmployeeScheduleDayIdIn(List.of(dayId)))
                .thenReturn(List.of(breakEntity));

        List<EmployeeScheduleResponse> result = useCase.execute(employeeId, principal);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Horário Padrão");
        assertThat(result.getFirst().days()).hasSize(1);
        assertThat(result.getFirst().days().getFirst().breaks()).hasSize(1);
    }
}
