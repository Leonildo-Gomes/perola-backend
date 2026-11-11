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
class ListEmployeesUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BusinessEmployeeEnrichmentService businessEmployeeEnrichmentService;

    @InjectMocks
    private ListEmployeesUseCase listEmployeesUseCase;

    @Test
    @DisplayName("Deve listar todos os colaboradores do salão (ativos e inativos) enriquecidos com sucesso")
    void shouldListAllEmployeesSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        EmployeeEntity e1 = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        e1.setId(UUID.randomUUID());

        EmployeeEntity e2 = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("João Mendes")
                .isActive(false)
                .build();
        e2.setId(UUID.randomUUID());

        List<EmployeeEntity> entities = List.of(e1, e2);
        BusinessEmployeeResponse r1 = new BusinessEmployeeResponse(
                e1.getId(), businessId, null, "Maria Silva", null, null, null, null, null, List.of(), true, Instant.now(), Instant.now()
        );
        BusinessEmployeeResponse r2 = new BusinessEmployeeResponse(
                e2.getId(), businessId, null, "João Mendes", null, null, null, null, null, List.of(), false, Instant.now(), Instant.now()
        );

        when(employeeRepository.findAllByBusinessId(businessId)).thenReturn(entities);
        when(businessEmployeeEnrichmentService.enrich(businessId, entities)).thenReturn(List.of(r1, r2));

        // Act
        List<BusinessEmployeeResponse> result = listEmployeesUseCase.execute(businessId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).displayName()).isEqualTo("Maria Silva");
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(1).displayName()).isEqualTo("João Mendes");
        assertThat(result.get(1).isActive()).isFalse();

        verify(employeeRepository).findAllByBusinessId(businessId);
        verify(businessEmployeeEnrichmentService).enrich(businessId, entities);
    }
}
