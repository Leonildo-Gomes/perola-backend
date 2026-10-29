package cv.perola.modules.schedule.usecase;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEmployeeScheduleUseCaseTest {

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleDayRepository scheduleDayRepository;

    @Mock
    private EmployeeScheduleBreakRepository breakRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    private GetEmployeeScheduleUseCase useCase;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(businessId);
        useCase = new GetEmployeeScheduleUseCase(
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
    @DisplayName("Deve obter um perfil de horário específico por scheduleId e employeeId")
    void shouldGetEmployeeScheduleByIdSuccessfully() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        UUID dayId = UUID.randomUUID();
        EmployeeScheduleEntity scheduleEntity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário de Dezembro")
                .isDefault(false)
                .isActive(true)
                .build();
        scheduleEntity.setId(scheduleId);

        EmployeeScheduleDayEntity dayEntity = EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(scheduleId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .isWorking(true)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(18, 0))
                .build();
        dayEntity.setId(dayId);

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.of(scheduleEntity));
        when(scheduleDayRepository.findAllByEmployeeScheduleIdOrderByDayOfWeekAsc(scheduleId)).thenReturn(List.of(dayEntity));
        when(breakRepository.findAllByEmployeeScheduleDayIdIn(List.of(dayId))).thenReturn(Collections.emptyList());

        EmployeeScheduleResponse response = useCase.execute(employeeId, scheduleId, principal);

        assertThat(response.id()).isEqualTo(scheduleId);
        assertThat(response.name()).isEqualTo("Horário de Dezembro");
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().getFirst().startTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando perfil não existir para o colaborador")
    void shouldThrowResourceNotFoundExceptionWhenScheduleNotFound() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(employeeId, scheduleId, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
