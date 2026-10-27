package cv.perola.modules.identity.usecase;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Test
    @DisplayName("Deve autenticar com sucesso quando as credenciais forem válidas e conta ativa")
    void shouldAuthenticateSuccessfullyWhenCredentialsAreValid() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LoginRequest request = new LoginRequest("teste@perola.cv", "Password123!");
        UserEntity user = UserEntity.builder()
                .name("Maria Silva")
                .email("teste@perola.cv")
                .passwordHash("hashedSecret")
                .isActive(true)
                .build();
        user.setId(userId);

        UserProfileResponse userProfile = new UserProfileResponse(userId, "Maria Silva", "teste@perola.cv", "+2389912345", null, List.of(UserRole.CUSTOMER), List.of());
        AuthResponse expectedResponse = AuthResponse.of("access-token", "refresh-token", 3600L, userProfile);

        when(userRepository.findByEmail("teste@perola.cv")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashedSecret")).thenReturn(true);
        when(tokenService.issueTokens(user)).thenReturn(expectedResponse);

        // Act
        AuthResponse response = authenticateUserUseCase.execute(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(tokenService).issueTokens(user);
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando o email não for encontrado")
    void shouldThrowBadCredentialsExceptionWhenUserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest("desconhecido@perola.cv", "Password123!");
        when(userRepository.findByEmail("desconhecido@perola.cv")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou palavra-passe incorretos.");

        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException quando o utilizador estiver desativado")
    void shouldThrowAuthenticationFailedExceptionWhenUserIsInactive() {
        // Arrange
        LoginRequest request = new LoginRequest("inativo@perola.cv", "Password123!");
        UserEntity inactiveUser = UserEntity.builder()
                .name("Utilizador Inativo")
                .email("inativo@perola.cv")
                .passwordHash("hashedSecret")
                .isActive(false)
                .build();
        inactiveUser.setId(UUID.randomUUID());
        when(userRepository.findByEmail("inativo@perola.cv")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("A sua conta encontra-se desativada.");

        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando o password hash for nulo (ex: conta apenas Google)")
    void shouldThrowBadCredentialsExceptionWhenPasswordHashIsNull() {
        // Arrange
        LoginRequest request = new LoginRequest("google@perola.cv", "Password123!");
        UserEntity userWithoutPassword = UserEntity.builder()
                .name("Utilizador Social")
                .email("google@perola.cv")
                .passwordHash(null)
                .isActive(true)
                .build();
        userWithoutPassword.setId(UUID.randomUUID());
        when(userRepository.findByEmail("google@perola.cv")).thenReturn(Optional.of(userWithoutPassword));

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou palavra-passe incorretos.");

        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando a palavra-passe não coincidir")
    void shouldThrowBadCredentialsExceptionWhenPasswordDoesNotMatch() {
        // Arrange
        LoginRequest request = new LoginRequest("teste@perola.cv", "SenhaErrada!");
        UserEntity user = UserEntity.builder()
                .name("Maria Silva")
                .email("teste@perola.cv")
                .passwordHash("hashedSecret")
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("teste@perola.cv")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada!", "hashedSecret")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou palavra-passe incorretos.");

        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    @DisplayName("Deve normalizar email com espaços e letras maiúsculas antes de consultar repositório")
    void shouldNormalizeEmailBeforeLookup() {
        // Arrange
        LoginRequest request = new LoginRequest("  TESTE.NORMALIZADO@PEROLA.CV  ", "Password123!");
        when(userRepository.findByEmail("teste.normalizado@perola.cv")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository).findByEmail("teste.normalizado@perola.cv");
    }
}
