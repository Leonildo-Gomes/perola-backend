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
class ListActiveEmployeesUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @InjectMocks
    private ListActiveEmployeesUseCase listActiveEmployeesUseCase;

    @Test
    @DisplayName("Deve listar exclusivamente os colaboradores ativos do salão com sucesso")
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

        when(employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of(e1));

        // Act
        List<EmployeeResponse> result = listActiveEmployeesUseCase.execute(businessId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).displayName()).isEqualTo("Maria Silva");
        assertThat(result.get(0).isActive()).isTrue();

        verify(employeeRepository).findAllByBusinessIdAndIsActiveTrue(businessId);
    }
}

