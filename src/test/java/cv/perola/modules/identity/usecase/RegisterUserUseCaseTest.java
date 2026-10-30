package cv.perola.modules.identity.usecase;

import cv.perola.core.exception.ConflictException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.mapper.UserMapper;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import cv.perola.modules.identity.service.TokenService;
import cv.perola.modules.identity.validator.user.RegisterUserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    private RegisterUserValidator registerUserValidator;
    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        registerUserValidator = new RegisterUserValidator(userRepository);
        registerUserUseCase = new RegisterUserUseCase(
                userRepository,
                userRoleRepository,
                passwordEncoder,
                tokenService,
                userMapper,
                registerUserValidator
        );
    }

    @Test
    @DisplayName("Deve registar utilizador com sucesso, atribuir papel CUSTOMER e emitir tokens")
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Carla Semedo", "carla@perola.cv", "+2389912345", "SegredoForte123!");
        UUID savedUserId = UUID.randomUUID();
        UserEntity savedUser = UserEntity.builder()
                .name("Carla Semedo")
                .email("carla@perola.cv")
                .phone("+2389912345")
                .passwordHash("hashedPwd")
                .isActive(true)
                .build();
        savedUser.setId(savedUserId);

        UserProfileResponse profileResponse = new UserProfileResponse(savedUserId, "Carla Semedo", "carla@perola.cv", "+2389912345", null, List.of(UserRole.CUSTOMER), List.of());
        AuthResponse expectedAuthResponse = AuthResponse.of("jwt-token", "refresh-token", 3600L, profileResponse);

        when(userRepository.existsByEmail("carla@perola.cv")).thenReturn(false);
        when(userRepository.existsByPhone("+2389912345")).thenReturn(false);
        when(passwordEncoder.encode("SegredoForte123!")).thenReturn("hashedPwd");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(tokenService.issueTokens(savedUser)).thenReturn(expectedAuthResponse);

        // Act
        AuthResponse response = registerUserUseCase.execute(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt-token");

        ArgumentCaptor<UserRoleEntity> roleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(roleCaptor.getValue().getUserId()).isEqualTo(savedUserId);
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o email já estiver registado")
    void shouldThrowConflictExceptionWhenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Carla Semedo", "duplicado@perola.cv", "+2389912345", "SegredoForte123!");
        when(userRepository.existsByEmail("duplicado@perola.cv")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> registerUserUseCase.execute(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("O email 'duplicado@perola.cv' já se encontra registado.");

        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o número de telefone já estiver registado")
    void shouldThrowConflictExceptionWhenPhoneAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Carla Semedo", "novo@perola.cv", "+2389999999", "SegredoForte123!");
        when(userRepository.existsByEmail("novo@perola.cv")).thenReturn(false);
        when(userRepository.existsByPhone("+2389999999")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> registerUserUseCase.execute(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("O telefone '+2389999999' já se encontra associado a outra conta.");

        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
        verify(tokenService, never()).issueTokens(any());
    }
}
