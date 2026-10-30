package cv.perola.modules.schedule.usecase;

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
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.HolidayDecisionRequest;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import cv.perola.modules.schedule.mapper.MonthlyScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeMonthlyScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import cv.perola.modules.schedule.validator.monthly.OpenMonthlyScheduleValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMonthlyScheduleUseCaseTest {

        @Mock
        private EmployeeMonthlyScheduleRepository monthlyScheduleRepository;

        @Mock
        private EmployeeScheduleRepository scheduleRepository;

        @Mock
        private EmployeeTimeOffRepository timeOffRepository;

        @Mock
        private BusinessEmployeeQueryService employeeQueryService;

        private OpenMonthlyScheduleUseCase useCase;

        private final UUID businessId = UUID.randomUUID();
        private final UUID employeeId = UUID.randomUUID();
        private final UUID scheduleId = UUID.randomUUID();
        private final UUID ownerUserId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                TenantContext.setTenantId(businessId);
                useCase = new OpenMonthlyScheduleUseCase(
                                monthlyScheduleRepository,
                                scheduleRepository,
                                timeOffRepository,
                                employeeQueryService,
                                new MonthlyScheduleMapper(),
                                new OpenMonthlyScheduleValidator());
        }

        @AfterEach
        void tearDown() {
                TenantContext.clear();
        }

        @Test
        @DisplayName("Deve abrir a agenda mensal vinculando o scheduleId e criando folga para feriado com workOnHoliday=false")
        void shouldOpenMonthlyScheduleAndCreateHolidayTimeOffWhenNotWorkingOnHoliday() {
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now());
                UserPrincipal principal = new UserPrincipal(
                                ownerUserId, "maria@perola.cv", "hash",
                                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true);

                EmployeeScheduleEntity schedule = EmployeeScheduleEntity.builder()
                                .employeeId(employeeId)
                                .name("Horário Padrão")
                                .isDefault(true)
                                .isActive(true)
                                .build();
                schedule.setId(scheduleId);

                OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                                scheduleId,
                                2026,
                                11,
                                List.of(
                                                new HolidayDecisionRequest(LocalDate.of(2026, 11, 1), false),
                                                new HolidayDecisionRequest(LocalDate.of(2026, 11, 15), true)));

                when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
                when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId))
                                .thenReturn(Optional.of(schedule));
                when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 11))
                                .thenReturn(Optional.empty());
                when(monthlyScheduleRepository.save(any(EmployeeMonthlyScheduleEntity.class))).thenAnswer(inv -> {
                        EmployeeMonthlyScheduleEntity m = inv.getArgument(0);
                        m.setId(UUID.randomUUID());
                        return m;
                });

                MonthlyScheduleResponse response = useCase.execute(employeeId, request, principal);

                assertThat(response.employeeId()).isEqualTo(employeeId);
                assertThat(response.scheduleId()).isEqualTo(scheduleId);
                assertThat(response.scheduleName()).isEqualTo("Horário Padrão");
                assertThat(response.year()).isEqualTo(2026);
                assertThat(response.month()).isEqualTo(11);
                assertThat(response.isOpened()).isTrue();

                verify(timeOffRepository).deleteAllByEmployeeIdAndTypeAndStartDateBetween(
                                employeeId, TimeOffType.HOLIDAY, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

                ArgumentCaptor<EmployeeTimeOffEntity> captor = ArgumentCaptor.forClass(EmployeeTimeOffEntity.class);
                verify(timeOffRepository).save(captor.capture());
                EmployeeTimeOffEntity savedTimeOff = captor.getValue();
                assertThat(savedTimeOff.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
                assertThat(savedTimeOff.getType()).isEqualTo(TimeOffType.HOLIDAY);
                assertThat(savedTimeOff.isAllDay()).isTrue();
        }

        @Test
        @DisplayName("Deve atualizar o scheduleId quando o mês já estava aberto anteriormente")
        void shouldUpdateExistingMonthlyScheduleWithNewScheduleProfile() {
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now());
                UserPrincipal principal = new UserPrincipal(
                                ownerUserId, "maria@perola.cv", "hash",
                                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true);

                EmployeeScheduleEntity decSchedule = EmployeeScheduleEntity.builder()
                                .employeeId(employeeId)
                                .name("Horário de Dezembro")
                                .isDefault(false)
                                .isActive(true)
                                .build();
                decSchedule.setId(scheduleId);

                EmployeeMonthlyScheduleEntity existingMonthly = EmployeeMonthlyScheduleEntity.builder()
                                .employeeId(employeeId)
                                .employeeScheduleId(UUID.randomUUID())
                                .year(2026)
                                .month(12)
                                .isOpened(true)
                                .openedAt(Instant.now())
                                .build();
                existingMonthly.setId(UUID.randomUUID());

                OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                                scheduleId, 2026, 12, List.of());

                when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
                when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId))
                                .thenReturn(Optional.of(decSchedule));
                when(monthlyScheduleRepository.findByEmployeeIdAndYearAndMonth(employeeId, 2026, 12))
                                .thenReturn(Optional.of(existingMonthly));
                when(monthlyScheduleRepository.save(any(EmployeeMonthlyScheduleEntity.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                MonthlyScheduleResponse response = useCase.execute(employeeId, request, principal);

                assertThat(response.id()).isEqualTo(existingMonthly.getId());
                assertThat(response.scheduleId()).isEqualTo(scheduleId);
                assertThat(response.scheduleName()).isEqualTo("Horário de Dezembro");
                verify(timeOffRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar abertura mensal quando o perfil de horário estiver inativo")
        void shouldThrowValidationExceptionWhenScheduleProfileIsInactive() {
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now());
                UserPrincipal principal = new UserPrincipal(
                                ownerUserId, "maria@perola.cv", "hash",
                                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true);

                EmployeeScheduleEntity inactiveSchedule = EmployeeScheduleEntity.builder()
                                .employeeId(employeeId)
                                .name("Horário Arquivado")
                                .isDefault(false)
                                .isActive(false)
                                .build();
                inactiveSchedule.setId(scheduleId);

                OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                                scheduleId, 2026, 11, List.of());

                when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
                when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId))
                                .thenReturn(Optional.of(inactiveSchedule));

                assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Deve rejeitar abertura mensal quando data de feriado não pertencer ao mês/ano solicitado")
        void shouldThrowValidationExceptionWhenHolidayDateDoesNotBelongToMonth() {
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now());
                UserPrincipal principal = new UserPrincipal(
                                ownerUserId, "maria@perola.cv", "hash",
                                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true);

                EmployeeScheduleEntity schedule = EmployeeScheduleEntity.builder()
                                .employeeId(employeeId)
                                .name("Horário Padrão")
                                .isDefault(true)
                                .isActive(true)
                                .build();
                schedule.setId(scheduleId);

                OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                                scheduleId,
                                2026,
                                11,
                                List.of(new HolidayDecisionRequest(LocalDate.of(2026, 12, 25), false)));

                when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
                when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId))
                                .thenReturn(Optional.of(schedule));

                assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("não pertence ao período");
        }

        @Test
        @DisplayName("Deve rejeitar com TenantAccessDeniedException quando funcionário tenta abrir agenda de outro colega")
        void shouldThrowTenantAccessDeniedExceptionWhenEmployeeOpensAnotherEmployeeMonth() {
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now());
                UserPrincipal otherPrincipal = new UserPrincipal(
                                UUID.randomUUID(), "outro@perola.cv", "hash",
                                List.of("EMPLOYEE"), List.of(new TenantMembership(businessId, "EMPLOYEE")), true);

                OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                                scheduleId, 2026, 11, List.of());

                when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

                assertThatThrownBy(() -> useCase.execute(employeeId, request, otherPrincipal))
                                .isInstanceOf(TenantAccessDeniedException.class);
        }

        @Test
        @DisplayName("Deve rejeitar com ResourceNotFoundException quando perfil de horário não existir")
        void shouldThrowResourceNotFoundExceptionWhenScheduleDoesNotExist() {
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, ownerUserId, "Maria", true, Instant.now(), Instant.now());
                UserPrincipal principal = new UserPrincipal(
                                ownerUserId, "maria@perola.cv", "hash",
                                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true);

                OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                                scheduleId, 2026, 11, List.of());

                when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
                when(scheduleRepository.findByIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> useCase.execute(employeeId, request, principal))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

}
