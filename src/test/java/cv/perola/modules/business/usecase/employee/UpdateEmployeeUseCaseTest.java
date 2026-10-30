package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.UpdateEmployeeRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @InjectMocks
    private UpdateEmployeeUseCase updateEmployeeUseCase;

    @Test
    @DisplayName("Deve atualizar dados do profissional com sucesso")
    void shouldUpdateEmployeeSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity existing = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Ana Antiga")
                .isActive(true)
                .build();
        existing.setId(employeeId);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Ana Nova - Master Stylist", true);

        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EmployeeResponse response = updateEmployeeUseCase.execute(businessId, employeeId, request);

        // Assert
        assertThat(response.displayName()).isEqualTo("Ana Nova - Master Stylist");
        verify(employeeRepository).save(existing);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando profissional não for encontrado")
    void shouldThrowResourceNotFoundWhenEmployeeDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Novo Nome", null);

        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateEmployeeUseCase.execute(businessId, employeeId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profissional não encontrado");

        verify(employeeRepository, never()).save(any());
    }
}
