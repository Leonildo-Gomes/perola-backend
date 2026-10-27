package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import cv.perola.modules.schedule.mapper.TimeOffMapper;
import cv.perola.modules.schedule.repository.EmployeeTimeOffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTimeOffUseCaseTest {

    @Mock
    private EmployeeTimeOffRepository timeOffRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    @Mock
    private TimeOffMapper timeOffMapper;

    @InjectMocks
    private ListTimeOffUseCase listTimeOffUseCase;

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
    @DisplayName("Deve listar ausências ordenadas para OWNER")
    void shouldListTimeOffSuccessfullyForOwner() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        EmployeeTimeOffEntity entity = EmployeeTimeOffEntity.builder()
                .employeeId(employeeId)
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 15))
                .isAllDay(true)
                .type(TimeOffType.VACATION)
                .build();
        TimeOffResponse expectedResponse = new TimeOffResponse(
                UUID.randomUUID(), employeeId, entity.getStartDate(), entity.getEndDate(), true,
                null, null, TimeOffType.VACATION, null, Instant.now(), Instant.now()
        );

        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(timeOffRepository.findAllByEmployeeIdOrderByStartDateAscStartTimeAsc(employeeId)).thenReturn(List.of(entity));
        when(timeOffMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        List<TimeOffResponse> result = listTimeOffUseCase.execute(employeeId, principal);

        // Assert
        assertThat(result).hasSize(1).contains(expectedResponse);
        verify(timeOffRepository).findAllByEmployeeIdOrderByStartDateAscStartTimeAsc(employeeId);
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando EMPLOYEE tenta consultar ausências de outro")
    void shouldThrowAccessDeniedWhenEmployeeConsultsAnother() {
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
        assertThatThrownBy(() -> listTimeOffUseCase.execute(employeeId, principal))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("O colaborador só pode consultar as suas próprias ausências.");
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
        assertThatThrownBy(() -> listTimeOffUseCase.execute(employeeId, principal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Colaborador não encontrado.");
    }
}
