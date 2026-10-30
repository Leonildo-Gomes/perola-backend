package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.validator.employee.CreateEmployeeValidator;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEmployeeUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private UserAccountService userAccountService;

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    private CreateEmployeeValidator createEmployeeValidator;
    private CreateEmployeeUseCase createEmployeeUseCase;

    @BeforeEach
    void setUp() {
        createEmployeeValidator = new CreateEmployeeValidator(businessRepository, employeeRepository, userAccountService);
        createEmployeeUseCase = new CreateEmployeeUseCase(
                employeeRepository,
                userAccountService,
                employeeMapper,
                createEmployeeValidator
        );
    }

    @Test
    @DisplayName("Deve criar colaborador com utilizador associado e conceder papel EMPLOYEE")
    void shouldCreateEmployeeWithAssociatedUserAndAssignRole() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(userId, "Ana Cabral");

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(userAccountService.existsById(userId)).thenReturn(true);
        when(employeeRepository.existsByUserId(userId)).thenReturn(false);

        when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(inv -> {
            EmployeeEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        // Act
        EmployeeResponse response = createEmployeeUseCase.execute(businessId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.displayName()).isEqualTo("Ana Cabral");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.businessId()).isEqualTo(businessId);
        assertThat(response.isActive()).isTrue();

        verify(userAccountService).assignRole(userId, UserRole.EMPLOYEE);
        verify(employeeRepository).save(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Deve criar colaborador sem utilizador associado com sucesso")
    void shouldCreateEmployeeWithoutAssociatedUser() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(null, "Carlos Lima");

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(inv -> {
            EmployeeEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        // Act
        EmployeeResponse response = createEmployeeUseCase.execute(businessId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.displayName()).isEqualTo("Carlos Lima");
        assertThat(response.userId()).isNull();

        verify(userAccountService, never()).assignRole(any(), any());
        verify(employeeRepository).save(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando salão não existir")
    void shouldThrowResourceNotFoundWhenBusinessDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(null, "Carlos Lima");

        when(businessRepository.existsById(businessId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> createEmployeeUseCase.execute(businessId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Salão não encontrado");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando utilizador já for colaborador")
    void shouldThrowConflictWhenUserAlreadyRegisteredAsEmployee() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(userId, "Ana Cabral");

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(userAccountService.existsById(userId)).thenReturn(true);
        when(employeeRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createEmployeeUseCase.execute(businessId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já está registado como profissional");

        verify(employeeRepository, never()).save(any());
    }
}
