package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWeeklyScheduleUseCaseTest {

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleBreakRepository breakRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private GetWeeklyScheduleUseCase getWeeklyScheduleUseCase;

    private UUID businessId;
    private UUID employeeId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve retornar expediente semanal e pausas com sucesso para OWNER")
    void shouldReturnWeeklyScheduleSuccessfullyForOwner() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        UUID scheduleId = UUID.randomUUID();
        EmployeeScheduleEntity schedule = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .isActive(true)
                .build();
        schedule.setId(scheduleId);

        EmployeeScheduleBreakEntity breakEntity = EmployeeScheduleBreakEntity.builder()
                .id(UUID.randomUUID())
                .employeeScheduleId(scheduleId)
                .title("Almoço")
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .build();

        WeeklyScheduleItemResponse itemResponse = new WeeklyScheduleItemResponse(
                scheduleId, employeeId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true,
                List.of(new BreakItemResponse(breakEntity.getId(), "Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0)))
        );
        WeeklyScheduleResponse expectedResponse = new WeeklyScheduleResponse(employeeId, List.of(itemResponse));

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findAllByEmployeeIdOrderByDayOfWeekAsc(employeeId)).thenReturn(List.of(schedule));
        when(breakRepository.findAllByEmployeeScheduleIdIn(List.of(scheduleId))).thenReturn(List.of(breakEntity));
        when(scheduleMapper.toResponse(eq(schedule), any())).thenReturn(itemResponse);
        when(scheduleMapper.toResponse(eq(employeeId), any())).thenReturn(expectedResponse);

        // Act
        WeeklyScheduleResponse response = getWeeklyScheduleUseCase.execute(employeeId, principal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.employeeId()).isEqualTo(employeeId);
        verify(scheduleRepository).findAllByEmployeeIdOrderByDayOfWeekAsc(employeeId);
    }

    @Test
    @DisplayName("Deve retornar vazio quando colaborador não possui horários cadastrados")
    void shouldReturnEmptyScheduleWhenNoneConfigured() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        WeeklyScheduleResponse emptyResponse = new WeeklyScheduleResponse(employeeId, Collections.emptyList());

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findAllByEmployeeIdOrderByDayOfWeekAsc(employeeId)).thenReturn(Collections.emptyList());
        when(scheduleMapper.toResponse(employeeId, Collections.emptyList())).thenReturn(emptyResponse);

        // Act
        WeeklyScheduleResponse response = getWeeklyScheduleUseCase.execute(employeeId, principal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.schedules()).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando EMPLOYEE tenta consultar expediente de outro colaborador")
    void shouldThrowTenantAccessDeniedWhenEmployeeConsultsAnotherEmployee() {
        // Arrange
        UUID otherUserId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash", List.of("EMPLOYEE"),
                List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, otherUserId, "Outro", true, Instant.now(), Instant.now()
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> getWeeklyScheduleUseCase.execute(employeeId, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("O colaborador só pode aceder ao seu próprio expediente.");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando colaborador não existe")
    void shouldThrowResourceNotFoundWhenEmployeeDoesNotExist() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getWeeklyScheduleUseCase.execute(employeeId, principal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Colaborador não encontrado.");
    }
}
