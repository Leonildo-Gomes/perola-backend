package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEmployeeScheduleUseCaseTest {

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleDayRepository scheduleDayRepository;

    @Mock
    private EmployeeScheduleBreakRepository breakRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    private CreateEmployeeScheduleUseCase useCase;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(businessId);
        useCase = new CreateEmployeeScheduleUseCase(
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
    @DisplayName("Deve criar perfil de horário semanal com 7 dias e pausas com sucesso")
    void shouldCreateEmployeeScheduleWithSevenDaysAndBreaksSuccessfully() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        List<ScheduleDayItemRequest> days = buildValidSevenDays();
        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário Padrão", true, true, days
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(employeeId)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(EmployeeScheduleEntity.class))).thenAnswer(inv -> {
            EmployeeScheduleEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(scheduleDayRepository.save(any(EmployeeScheduleDayEntity.class))).thenAnswer(inv -> {
            EmployeeScheduleDayEntity d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(breakRepository.save(any(EmployeeScheduleBreakEntity.class))).thenAnswer(inv -> {
            EmployeeScheduleBreakEntity b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        EmployeeScheduleResponse response = useCase.execute(employeeId, request, principal);

        assertThat(response.name()).isEqualTo("Horário Padrão");
        assertThat(response.isDefault()).isTrue();
        assertThat(response.days()).hasSize(7);
        assertThat(response.days().getFirst().dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.days().getFirst().breaks()).hasSize(1);
        assertThat(response.days().get(6).dayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(response.days().get(6).isWorking()).isFalse();
        assertThat(response.days().get(6).startTime()).isNull();
    }

    @Test
    @DisplayName("Deve desmarcar perfil padrão anterior ao criar novo perfil marcado como isDefault=true")
    void shouldUnsetPreviousDefaultWhenCreatingNewDefaultSchedule() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        EmployeeScheduleEntity prevDefault = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Antigo Padrão")
                .isDefault(true)
                .isActive(true)
                .build();
        prevDefault.setId(UUID.randomUUID());

        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Novo Padrão", true, true, buildValidSevenDays()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(employeeId))
                .thenReturn(Optional.of(prevDefault));
        when(scheduleRepository.save(any(EmployeeScheduleEntity.class))).thenAnswer(inv -> {
            EmployeeScheduleEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(scheduleDayRepository.save(any(EmployeeScheduleDayEntity.class))).thenAnswer(inv -> {
            EmployeeScheduleDayEntity d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(breakRepository.save(any(EmployeeScheduleBreakEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(employeeId, request, principal);

        assertThat(prevDefault.isDefault()).isFalse();
        verify(scheduleRepository).saveAndFlush(prevDefault);
    }

    @Test
    @DisplayName("Deve rejeitar com ValidationException quando menos de 7 dias forem informados")
    void shouldThrowValidationExceptionWhenFewerThanSevenDaysProvided() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        List<ScheduleDayItemRequest> onlyOneDay = List.of(
                new ScheduleDayItemRequest(DayOfWeek.MONDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0), Collections.emptyList())
        );
        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Incompleto", true, true, onlyOneDay
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("7 dias da semana");
    }

    @Test
    @DisplayName("Deve rejeitar com ValidationException quando dia de folga contiver horas ou pausas")
    void shouldThrowValidationExceptionWhenNonWorkingDayHasTimesOrBreaks() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        List<ScheduleDayItemRequest> days = new ArrayList<>(buildValidSevenDays());
        days.set(6, new ScheduleDayItemRequest(
                DayOfWeek.SUNDAY, false, LocalTime.of(9, 0), LocalTime.of(12, 0), Collections.emptyList()
        ));

        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário Inválido", true, true, days
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("folga");
    }

    @Test
    @DisplayName("Deve rejeitar com ValidationException quando pausa estiver fora do expediente ou sobreposta")
    void shouldThrowValidationExceptionWhenBreakIsOutsideWorkingHours() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        List<ScheduleDayItemRequest> days = new ArrayList<>(buildValidSevenDays());
        days.set(0, new ScheduleDayItemRequest(
                DayOfWeek.MONDAY,
                true,
                LocalTime.of(8, 30),
                LocalTime.of(16, 0),
                List.of(new BreakItemRequest("Jantar Fora de Hora", LocalTime.of(15, 30), LocalTime.of(17, 0)))
        ));

        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário Inválido", true, true, days
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalmente contida no turno");
    }

    @Test
    @DisplayName("Deve rejeitar com TenantAccessDeniedException quando funcionário tenta criar horário para outro colega")
    void shouldThrowTenantAccessDeniedExceptionWhenEmployeeModifiesAnotherEmployeeSchedule() {
        UUID otherUserId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal otherEmployeePrincipal = new UserPrincipal(
                otherUserId, "outro@perola.cv", "hash",
                List.of("EMPLOYEE"), List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );

        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário Padrão", true, true, buildValidSevenDays()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> useCase.execute(employeeId, request, otherEmployeePrincipal))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar com ResourceNotFoundException quando colaborador não existir")
    void shouldThrowResourceNotFoundExceptionWhenEmployeeDoesNotExist() {
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );
        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário Padrão", true, true, buildValidSevenDays()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    static List<ScheduleDayItemRequest> buildValidSevenDays() {
        return List.of(
                new ScheduleDayItemRequest(
                        DayOfWeek.MONDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0),
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 30), LocalTime.of(13, 0)))
                ),
                new ScheduleDayItemRequest(DayOfWeek.TUESDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0), Collections.emptyList()),
                new ScheduleDayItemRequest(DayOfWeek.WEDNESDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0), Collections.emptyList()),
                new ScheduleDayItemRequest(DayOfWeek.THURSDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0), Collections.emptyList()),
                new ScheduleDayItemRequest(DayOfWeek.FRIDAY, true, LocalTime.of(8, 30), LocalTime.of(16, 0), Collections.emptyList()),
                new ScheduleDayItemRequest(DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0), Collections.emptyList()),
                new ScheduleDayItemRequest(DayOfWeek.SUNDAY, false, null, null, Collections.emptyList())
        );
    }
}
