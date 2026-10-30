package cv.perola.modules.business.usecase.employee;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @InjectMocks
    private ListEmployeesUseCase listEmployeesUseCase;

    @Test
    @DisplayName("Deve listar colaboradores ativos do salão com sucesso")
    void shouldListActiveEmployeesSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        EmployeeEntity e1 = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        e1.setId(UUID.randomUUID());
        e1.setCreatedAt(Instant.now());
        e1.setUpdatedAt(Instant.now());

        EmployeeEntity e2 = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("João Mendes")
                .isActive(true)
                .build();
        e2.setId(UUID.randomUUID());
        e2.setCreatedAt(Instant.now());
        e2.setUpdatedAt(Instant.now());

        when(employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of(e1, e2));

        // Act
        List<EmployeeResponse> result = listEmployeesUseCase.execute(businessId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).displayName()).isEqualTo("Maria Silva");
        assertThat(result.get(1).displayName()).isEqualTo("João Mendes");

        verify(employeeRepository).findAllByBusinessIdAndIsActiveTrue(businessId);
    }
}
