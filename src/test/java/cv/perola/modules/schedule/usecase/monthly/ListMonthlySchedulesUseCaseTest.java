package cv.perola.modules.schedule.usecase.monthly;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.mapper.MonthlyScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeMonthlyScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMonthlySchedulesUseCaseTest {

    @Mock
    private EmployeeMonthlyScheduleRepository monthlyScheduleRepository;

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    private ListMonthlySchedulesUseCase useCase;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID scheduleId1 = UUID.randomUUID();
    private final UUID scheduleId2 = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(businessId);
        useCase = new ListMonthlySchedulesUseCase(
                monthlyScheduleRepository,
                scheduleRepository,
                employeeQueryService,
                new MonthlyScheduleMapper()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve listar aberturas mensais do profissional ordenadas por mês com os nomes dos horários")
    void shouldListMonthlySchedulesByYearSuccessfully() {
        // Arrange
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", null, true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        EmployeeMonthlyScheduleEntity nov = EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(scheduleId1)
                .year(2026)
                .month(11)
                .isOpened(true)
                .openedAt(Instant.parse("2026-10-29T10:00:00Z"))
                .build();
        nov.setId(UUID.randomUUID());

        EmployeeMonthlyScheduleEntity dez = EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(scheduleId2)
                .year(2026)
                .month(12)
                .isOpened(true)
                .openedAt(Instant.parse("2026-10-29T11:00:00Z"))
                .build();
        dez.setId(UUID.randomUUID());

        EmployeeScheduleEntity schedule1 = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Normal")
                .isDefault(true)
                .isActive(true)
                .build();
        schedule1.setId(scheduleId1);

        EmployeeScheduleEntity schedule2 = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Especial de Dezembro")
                .isDefault(false)
                .isActive(true)
                .build();
        schedule2.setId(scheduleId2);

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(monthlyScheduleRepository.findAllByEmployeeIdAndYearOrderByMonthAsc(employeeId, 2026))
                .thenReturn(List.of(nov, dez));
        when(scheduleRepository.findAllById(Set.of(scheduleId1, scheduleId2)))
                .thenReturn(List.of(schedule1, schedule2));

        // Act
        List<MonthlyScheduleResponse> response = useCase.execute(employeeId, 2026, principal);

        // Assert
        assertThat(response).hasSize(2);

        MonthlyScheduleResponse first = response.get(0);
        assertThat(first.month()).isEqualTo(11);
        assertThat(first.scheduleName()).isEqualTo("Horário Normal");
        assertThat(first.isOpened()).isTrue();

        MonthlyScheduleResponse second = response.get(1);
        assertThat(second.month()).isEqualTo(12);
        assertThat(second.scheduleName()).isEqualTo("Horário Especial de Dezembro");
        assertThat(second.isOpened()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nenhum mês estiver configurado para o ano")
    void shouldReturnEmptyListWhenNoMonthsConfigured() {
        // Arrange
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", null, true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(monthlyScheduleRepository.findAllByEmployeeIdAndYearOrderByMonthAsc(employeeId, 2026))
                .thenReturn(Collections.emptyList());

        // Act
        List<MonthlyScheduleResponse> response = useCase.execute(employeeId, 2026, principal);

        // Assert
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o ano for inválido")
    void shouldThrowValidationExceptionWhenYearIsInvalid() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(employeeId, 2020, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("compreendido entre 2024 e 2100");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o colaborador não for encontrado")
    void shouldThrowResourceNotFoundExceptionWhenEmployeeNotFound() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(employeeId, 2026, principal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Colaborador não encontrado");
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando o colaborador pertencer a outro salão")
    void shouldThrowTenantAccessDeniedExceptionWhenEmployeeBelongsToAnotherBusiness() {
        // Arrange
        UUID otherBusinessId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, otherBusinessId, ownerUserId, "Maria", null, true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(employeeId, 2026, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("não pertence ao salão ativo");
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando funcionário tentar consultar agenda de outro profissional")
    void shouldThrowTenantAccessDeniedExceptionWhenEmployeeTriesToAccessAnotherEmployeeSchedule() {
        // Arrange
        UUID anotherUserId = UUID.randomUUID();
        UUID callerUserId = UUID.randomUUID();

        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, anotherUserId, "Outro Funcionario", null, true, Instant.now(), Instant.now()
        );
        UserPrincipal callerEmployeePrincipal = new UserPrincipal(
                callerUserId, "staff@perola.cv", "hash",
                List.of("EMPLOYEE"), List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(employeeId, 2026, callerEmployeePrincipal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("própria agenda mensal");
    }
}

