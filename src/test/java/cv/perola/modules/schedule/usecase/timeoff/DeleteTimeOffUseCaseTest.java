package cv.perola.modules.schedule.usecase.timeoff;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTimeOffUseCaseTest {

    @Mock
    private EmployeeTimeOffRepository timeOffRepository;

    @Mock
    private BusinessEmployeeQueryService employeeQueryService;

    @InjectMocks
    private DeleteTimeOffUseCase deleteTimeOffUseCase;

    private UUID businessId;
    private UUID employeeId;
    private UUID timeOffId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        timeOffId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve remover ausência com sucesso quando OWNER")
    void shouldDeleteTimeOffSuccessfullyWhenOwner() {
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
                .type(TimeOffType.VACATION)
                .build();
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(timeOffRepository.findByIdAndEmployeeId(timeOffId, employeeId)).thenReturn(Optional.of(entity));

        // Act
        deleteTimeOffUseCase.execute(employeeId, timeOffId, principal);

        // Assert
        verify(timeOffRepository).delete(entity);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando registo de ausência não existir")
    void shouldThrowResourceNotFoundWhenTimeOffNotFound() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                userId, "owner@perola.cv", "hash", List.of("OWNER"),
                List.of(new TenantMembership(businessId, "OWNER")), true
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        when(employeeQueryService.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(timeOffRepository.findByIdAndEmployeeId(timeOffId, employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deleteTimeOffUseCase.execute(employeeId, timeOffId, principal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Registo de ausência não encontrado.");
    }
}
