package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.service.BusinessEmployeeEnrichmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleEmployeeStatusUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BusinessEmployeeEnrichmentService businessEmployeeEnrichmentService;

    @InjectMocks
    private ToggleEmployeeStatusUseCase useCase;

    @Test
    @DisplayName("Deve alternar status do colaborador de ativo (true) para inativo (false)")
    void shouldToggleFromActiveToInactive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Carlos Cabeleireiro")
                .isActive(true)
                .build();

        BusinessEmployeeResponse expectedResponse = new BusinessEmployeeResponse(
                employeeId, businessId, null, "Carlos Cabeleireiro", null, null, null, null, null, List.of(), false, Instant.now(), Instant.now()
        );

        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessEmployeeEnrichmentService.enrichSingle(eq(businessId), any(EmployeeEntity.class)))
                .thenReturn(expectedResponse);

        // Act
        BusinessEmployeeResponse response = useCase.execute(businessId, employeeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();
        assertThat(employee.isActive()).isFalse();

        verify(employeeRepository).findByIdAndBusinessId(employeeId, businessId);
        verify(employeeRepository).save(employee);
        verify(businessEmployeeEnrichmentService).enrichSingle(businessId, employee);
    }

    @Test
    @DisplayName("Deve alternar status do colaborador de inativo (false) para ativo (true)")
    void shouldToggleFromInactiveToActive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Carlos Cabeleireiro")
                .isActive(false)
                .build();

        BusinessEmployeeResponse expectedResponse = new BusinessEmployeeResponse(
                employeeId, businessId, null, "Carlos Cabeleireiro", null, null, null, null, null, List.of(), true, Instant.now(), Instant.now()
        );

        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessEmployeeEnrichmentService.enrichSingle(eq(businessId), any(EmployeeEntity.class)))
                .thenReturn(expectedResponse);

        // Act
        BusinessEmployeeResponse response = useCase.execute(businessId, employeeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isTrue();
        assertThat(employee.isActive()).isTrue();

        verify(employeeRepository).findByIdAndBusinessId(employeeId, businessId);
        verify(employeeRepository).save(employee);
        verify(businessEmployeeEnrichmentService).enrichSingle(businessId, employee);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando colaborador não for encontrado")
    void shouldThrowWhenEmployeeNotFound() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(businessId, employeeId));

        verify(employeeRepository).findByIdAndBusinessId(employeeId, businessId);
    }
}
