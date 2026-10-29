package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMonthlyScheduleUseCaseTest {

    @Mock
    private EmployeeMonthlyScheduleRepository monthlyScheduleRepository;

    @Mock
    private EmployeeScheduleRepository scheduleRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    private GetMonthlyScheduleUseCase useCase;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(businessId);
        useCase = new GetMonthlyScheduleUseCase(
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
    @DisplayName("Deve consultar agenda mensal aberta retornando dados do horário associado")
    void shouldGetOpenedMonthlyScheduleSuccessfully() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        EmployeeMonthlyScheduleEntity monthlyEntity = EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(scheduleId)
                .year(2026)
                .month(11)
                .isOpened(true)
                .openedAt(Instant.parse("2026-10-29T18:00:00Z"))
                .build();
        monthlyEntity.setId(UUID.randomUUID());

        EmployeeScheduleEntity scheduleEntity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário Padrão")
                .isDefault(true)
                .isActive(true)
                .build();
        scheduleEntity.setId(scheduleId);

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 11))
                .thenReturn(Optional.of(monthlyEntity));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(scheduleEntity));

        MonthlyScheduleResponse response = useCase.execute(employeeId, 2026, 11, principal);

        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.scheduleId()).isEqualTo(scheduleId);
        assertThat(response.scheduleName()).isEqualTo("Horário Padrão");
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(11);
        assertThat(response.isOpened()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o mês ainda não tiver sido aberto")
    void shouldThrowResourceNotFoundExceptionWhenMonthIsNotOpened() {
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 11))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(employeeId, 2026, 11, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando mês ou ano forem inválidos")
    void shouldThrowValidationExceptionWhenMonthOrYearInvalid() {
        UserPrincipal principal = new UserPrincipal(
                ownerUserId, "maria@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );

        assertThatThrownBy(() -> useCase.execute(employeeId, 2026, 13, principal))
                .isInstanceOf(ValidationException.class);
    }
}
