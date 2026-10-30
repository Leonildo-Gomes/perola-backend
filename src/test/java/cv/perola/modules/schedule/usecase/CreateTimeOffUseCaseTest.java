package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import cv.perola.modules.schedule.mapper.TimeOffMapper;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import cv.perola.modules.schedule.validator.timeoff.CreateTimeOffValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTimeOffUseCaseTest {

    @Mock
    private EmployeeTimeOffRepository timeOffRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    @Mock
    private TimeOffMapper timeOffMapper;

    @Spy
    private CreateTimeOffValidator timeOffValidator = new CreateTimeOffValidator();

    @InjectMocks
    private CreateTimeOffUseCase createTimeOffUseCase;

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
    @DisplayName("Deve criar ausência de dia inteiro com sucesso para OWNER")
    void shouldSuccessfullyCreateTimeOffWhenUserIsOwner() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), true, null, null,
                TimeOffType.VACATION, "Férias anuais"
        );
        EmployeeTimeOffEntity entity = EmployeeTimeOffEntity.builder()
                .employeeId(employeeId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isAllDay(true)
                .type(TimeOffType.VACATION)
                .note("Férias anuais")
                .build();
        TimeOffResponse expectedResponse = new TimeOffResponse(
                UUID.randomUUID(), employeeId, request.startDate(), request.endDate(), true,
                null, null, TimeOffType.VACATION, "Férias anuais", Instant.now(), Instant.now()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(timeOffMapper.toEntity(employeeId, request)).thenReturn(entity);
        when(timeOffRepository.save(entity)).thenReturn(entity);
        when(timeOffMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        TimeOffResponse response = createTimeOffUseCase.execute(employeeId, request, principal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(TimeOffType.VACATION);
        verify(timeOffRepository).save(entity);
    }

    @Test
    @DisplayName("Deve criar ausência parcial com sucesso para o próprio EMPLOYEE")
    void shouldSuccessfullyCreateTimeOffWhenUserIsOwnEmployee() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash", List.of("EMPLOYEE"),
                List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10), false,
                LocalTime.of(14, 0), LocalTime.of(18, 0), TimeOffType.SICK_LEAVE, "Consulta"
        );
        EmployeeTimeOffEntity entity = EmployeeTimeOffEntity.builder()
                .employeeId(employeeId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isAllDay(false)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .type(TimeOffType.SICK_LEAVE)
                .note("Consulta")
                .build();
        TimeOffResponse expectedResponse = new TimeOffResponse(
                UUID.randomUUID(), employeeId, request.startDate(), request.endDate(), false,
                request.startTime(), request.endTime(), TimeOffType.SICK_LEAVE, "Consulta", Instant.now(), Instant.now()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(timeOffMapper.toEntity(employeeId, request)).thenReturn(entity);
        when(timeOffRepository.save(entity)).thenReturn(entity);
        when(timeOffMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        TimeOffResponse response = createTimeOffUseCase.execute(employeeId, request, principal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isAllDay()).isFalse();
        verify(timeOffRepository).save(entity);
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando EMPLOYEE tenta criar ausência para outro colaborador")
    void shouldThrowAccessDeniedWhenEmployeeUserTriesToCreateForAnother() {
        // Arrange
        UUID otherUserId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash", List.of("EMPLOYEE"),
                List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, otherUserId, "Outro", true, Instant.now(), Instant.now()
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), true, null, null,
                TimeOffType.VACATION, null
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> createTimeOffUseCase.execute(employeeId, request, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("O colaborador só pode registar ausências para o seu próprio perfil.");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando colaborador não existe")
    void shouldThrowResourceNotFoundWhenEmployeeDoesNotExist() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), true, null, null,
                TimeOffType.VACATION, null
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createTimeOffUseCase.execute(employeeId, request, principal))
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
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), true, null, null,
                TimeOffType.VACATION, null
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> createTimeOffUseCase.execute(employeeId, request, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("O colaborador especificado não pertence ao salão ativo.");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando endDate for anterior a startDate")
    void shouldThrowValidationExceptionWhenEndDateBeforeStartDate() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 10), true, null, null,
                TimeOffType.VACATION, null
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> createTimeOffUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não pode ser anterior à data de início");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando ausência parcial não informa horários")
    void shouldThrowValidationExceptionWhenPartialTimeOffMissingTimes() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10), false, null, null,
                TimeOffType.SICK_LEAVE, null
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> createTimeOffUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("são obrigatórios para ausências de período parcial");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando endTime não for posterior a startTime em ausência parcial")
    void shouldThrowValidationExceptionWhenPartialTimeOffEndTimeBeforeStartTime() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, userId, "Carlos", true, Instant.now(), Instant.now()
        );
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10), false,
                LocalTime.of(18, 0), LocalTime.of(14, 0), TimeOffType.SICK_LEAVE, null
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> createTimeOffUseCase.execute(employeeId, request, principal))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("deve ser posterior ao horário de início");
    }
}
