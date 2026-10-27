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
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.BreakItemResponse;
import cv.perola.modules.schedule.dto.SetWeeklyScheduleRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemRequest;
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
class SetWeeklyScheduleUseCaseTest {

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private EmployeeScheduleBreakRepository breakRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private SetWeeklyScheduleUseCase setWeeklyScheduleUseCase;

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
    @DisplayName("Deve configurar rotina semanal e pausas com sucesso quando utilizador é OWNER")
    void shouldSuccessfullySetWeeklyScheduleAndBreaksWhenUserIsOwner() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        BreakItemRequest breakReq = new BreakItemRequest("Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0));
        WeeklyScheduleItemRequest itemReq = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, List.of(breakReq)
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(itemReq));

        UUID scheduleId = UUID.randomUUID();
        EmployeeScheduleEntity scheduleEntity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .isActive(true)
                .build();
        scheduleEntity.setId(scheduleId);

        EmployeeScheduleBreakEntity breakEntity = EmployeeScheduleBreakEntity.builder()
                .employeeScheduleId(scheduleId)
                .title("Almoço")
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .build();

        WeeklyScheduleItemResponse itemResponse = new WeeklyScheduleItemResponse(
                scheduleId, employeeId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true,
                List.of(new BreakItemResponse(UUID.randomUUID(), "Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0)))
        );
        WeeklyScheduleResponse expectedResponse = new WeeklyScheduleResponse(employeeId, List.of(itemResponse));

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findAllByEmployeeId(employeeId)).thenReturn(Collections.emptyList());
        when(scheduleMapper.toEntity(employeeId, itemReq)).thenReturn(scheduleEntity);
        when(scheduleRepository.save(scheduleEntity)).thenReturn(scheduleEntity);
        when(scheduleMapper.toEntity(scheduleId, breakReq)).thenReturn(breakEntity);
        when(breakRepository.save(breakEntity)).thenReturn(breakEntity);
        when(scheduleMapper.toResponse(eq(scheduleEntity), any())).thenReturn(itemResponse);
        when(scheduleMapper.toResponse(eq(employeeId), any())).thenReturn(expectedResponse);

        // Act
        WeeklyScheduleResponse response = setWeeklyScheduleUseCase.execute(employeeId, request, principal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.employeeId()).isEqualTo(employeeId);
        verify(scheduleRepository).save(scheduleEntity);
        verify(breakRepository).save(breakEntity);
    }

    @Test
    @DisplayName("Deve configurar rotina semanal com sucesso quando utilizador é o próprio EMPLOYEE")
    void shouldSuccessfullySetWeeklyScheduleWhenUserIsOwnEmployee() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash", List.of("EMPLOYEE"),
                List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        WeeklyScheduleItemRequest itemReq = new WeeklyScheduleItemRequest(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), true, null
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(itemReq));

        EmployeeScheduleEntity scheduleEntity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isActive(true)
                .build();

        WeeklyScheduleItemResponse itemResponse = new WeeklyScheduleItemResponse(
                UUID.randomUUID(), employeeId, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), true, List.of()
        );
        WeeklyScheduleResponse expectedResponse = new WeeklyScheduleResponse(employeeId, List.of(itemResponse));

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(scheduleRepository.findAllByEmployeeId(employeeId)).thenReturn(Collections.emptyList());
        when(scheduleMapper.toEntity(employeeId, itemReq)).thenReturn(scheduleEntity);
        when(scheduleRepository.save(scheduleEntity)).thenReturn(scheduleEntity);
        when(scheduleMapper.toResponse(eq(scheduleEntity), any())).thenReturn(itemResponse);
        when(scheduleMapper.toResponse(eq(employeeId), any())).thenReturn(expectedResponse);

        // Act
        WeeklyScheduleResponse response = setWeeklyScheduleUseCase.execute(employeeId, request, principal);

        // Assert
        assertThat(response).isNotNull();
        verify(scheduleRepository).save(scheduleEntity);
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando EMPLOYEE tenta configurar expediente de outro colaborador")
    void shouldThrowTenantAccessDeniedWhenEmployeeUserTriesToEditAnotherEmployee() {
        // Arrange
        UUID otherUserId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash", List.of("EMPLOYEE"),
                List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, otherUserId, "Outro", true, Instant.now(), Instant.now()
        );
        WeeklyScheduleItemRequest itemReq = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, null
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(itemReq));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("O colaborador só pode gerir o seu próprio expediente.");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando colaborador não existe")
    void shouldThrowResourceNotFoundWhenEmployeeDoesNotExist() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(
                new WeeklyScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, null)
        ));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Colaborador não encontrado.");
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando colaborador pertence a outro salão")
    void shouldThrowTenantAccessDeniedWhenEmployeeBelongsToDifferentBusiness() {
        // Arrange
        UUID otherBusinessId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, otherBusinessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(
                new WeeklyScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, null)
        ));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("O colaborador especificado não pertence ao salão ativo.");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando endTime não for posterior a startTime")
    void shouldThrowValidationExceptionWhenEndTimeNotAfterStartTime() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        WeeklyScheduleItemRequest itemReq = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(8, 0), true, null
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(itemReq));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("deve ser posterior ao horário de início");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o mesmo dia da semana for repetido na lista")
    void shouldThrowValidationExceptionWhenDuplicateDayOfWeekConfigured() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        WeeklyScheduleItemRequest item1 = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), true, null
        );
        WeeklyScheduleItemRequest item2 = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0), true, null
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(item1, item2));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("foi configurado mais de uma vez");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando pausa não estiver totalmente contida no turno")
    void shouldThrowValidationExceptionWhenBreakNotContainedWithinWorkHours() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        BreakItemRequest invalidBreak = new BreakItemRequest("Almoço Tarde", LocalTime.of(17, 30), LocalTime.of(18, 30));
        WeeklyScheduleItemRequest itemReq = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, List.of(invalidBreak)
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(itemReq));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("deve estar totalmente contida no turno");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando houver sobreposição entre pausas")
    void shouldThrowValidationExceptionWhenBreaksOverlap() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        BreakItemRequest break1 = new BreakItemRequest("Pausa 1", LocalTime.of(12, 0), LocalTime.of(13, 0));
        BreakItemRequest break2 = new BreakItemRequest("Pausa 2", LocalTime.of(12, 30), LocalTime.of(13, 30));
        WeeklyScheduleItemRequest itemReq = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, List.of(break1, break2)
        );
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(itemReq));
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> setWeeklyScheduleUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sobrepõem-se no dia MONDAY");
    }
}
