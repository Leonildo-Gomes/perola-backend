package cv.perola.modules.identity.validator.user;

import cv.perola.core.exception.ConflictException;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegisterUserValidator validator;

    @Test
    @DisplayName("Deve validar com sucesso quando email e telefone não existem")
    void shouldValidateSuccessfullyWhenEmailAndPhoneDoNotExist() {
        RegisterRequest request = new RegisterRequest("Maria Silva", "maria@perola.cv", "9912345", "senhaSegura123");

        when(userRepository.existsByEmail("maria@perola.cv")).thenReturn(false);
        when(userRepository.existsByPhone("9912345")).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(request));

        verify(userRepository).existsByEmail("maria@perola.cv");
        verify(userRepository).existsByPhone("9912345");
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando email já existe")
    void shouldThrowConflictExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("Maria Silva", "maria@perola.cv", "9912345", "senhaSegura123");

        when(userRepository.existsByEmail("maria@perola.cv")).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(request));

        verify(userRepository).existsByEmail("maria@perola.cv");
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando telefone já existe")
    void shouldThrowConflictExceptionWhenPhoneExists() {
        RegisterRequest request = new RegisterRequest("Maria Silva", "maria@perola.cv", "9912345", "senhaSegura123");

        when(userRepository.existsByEmail("maria@perola.cv")).thenReturn(false);
        when(userRepository.existsByPhone("9912345")).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(request));

        verify(userRepository).existsByEmail("maria@perola.cv");
        verify(userRepository).existsByPhone("9912345");
    }
}

