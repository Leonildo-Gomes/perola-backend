package cv.perola.modules.business.validator.employee;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.identity.service.UserAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEmployeeValidatorTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private CreateEmployeeValidator validator;

    @Test
    @DisplayName("Deve validar com sucesso quando salão e utilizador são válidos")
    void shouldValidateSuccessfully() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(userId, "Maria");

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(userAccountService.existsById(userId)).thenReturn(true);
        when(employeeRepository.existsByUserId(userId)).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(businessId, request));

        verify(businessRepository).existsById(businessId);
        verify(userAccountService).existsById(userId);
        verify(employeeRepository).existsByUserId(userId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o salão não existir")
    void shouldThrowWhenBusinessNotFound() {
        UUID businessId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(null, "Maria");

        when(businessRepository.existsById(businessId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> validator.validate(businessId, request));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o utilizador já for colaborador")
    void shouldThrowWhenUserAlreadyEmployee() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(userId, "Maria");

        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(userAccountService.existsById(userId)).thenReturn(true);
        when(employeeRepository.existsByUserId(userId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(businessId, request));
    }
}
