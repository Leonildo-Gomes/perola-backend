package cv.perola.modules.identity.validator.auth;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserValidatorTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticateUserValidator validator;

    @Test
    @DisplayName("Deve validar utilizador ativo com credenciais corretas com sucesso")
    void shouldValidateSuccessfully() {
        UserEntity user = UserEntity.builder()
                .isActive(true)
                .passwordHash("hashedPass")
                .build();
        LoginRequest request = new LoginRequest("teste@perola.cv", "senha123");

        when(passwordEncoder.matches("senha123", "hashedPass")).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(user, request));

        verify(passwordEncoder).matches("senha123", "hashedPass");
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException quando conta estiver desativada")
    void shouldThrowWhenUserIsInactive() {
        UserEntity user = UserEntity.builder()
                .isActive(false)
                .passwordHash("hashedPass")
                .build();
        LoginRequest request = new LoginRequest("teste@perola.cv", "senha123");

        assertThrows(AuthenticationFailedException.class, () -> validator.validate(user, request));
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando senha for incorreta")
    void shouldThrowWhenPasswordDoesNotMatch() {
        UserEntity user = UserEntity.builder()
                .isActive(true)
                .passwordHash("hashedPass")
                .build();
        LoginRequest request = new LoginRequest("teste@perola.cv", "senhaErrada");

        when(passwordEncoder.matches("senhaErrada", "hashedPass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> validator.validate(user, request));
    }
}

