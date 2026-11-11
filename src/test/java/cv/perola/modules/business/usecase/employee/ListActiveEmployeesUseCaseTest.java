package cv.perola.modules.business.usecase.employee;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActiveEmployeesUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BusinessEmployeeEnrichmentService businessEmployeeEnrichmentService;

    @InjectMocks
    private ListActiveEmployeesUseCase listActiveEmployeesUseCase;

    @Test
    @DisplayName("Deve listar exclusivamente os colaboradores ativos do salão enriquecidos com sucesso")
    void shouldListActiveEmployeesSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        EmployeeEntity e1 = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        e1.setId(UUID.randomUUID());

        List<EmployeeEntity> entities = List.of(e1);
        BusinessEmployeeResponse r1 = new BusinessEmployeeResponse(
                e1.getId(), businessId, null, "Maria Silva", null, null, null, null, null, List.of(), true, Instant.now(), Instant.now()
        );

        when(employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(entities);
        when(businessEmployeeEnrichmentService.enrich(businessId, entities)).thenReturn(List.of(r1));

        // Act
        List<BusinessEmployeeResponse> result = listActiveEmployeesUseCase.execute(businessId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).displayName()).isEqualTo("Maria Silva");
        assertThat(result.get(0).isActive()).isTrue();

        verify(employeeRepository).findAllByBusinessIdAndIsActiveTrue(businessId);
        verify(businessEmployeeEnrichmentService).enrich(businessId, entities);
    }
}
