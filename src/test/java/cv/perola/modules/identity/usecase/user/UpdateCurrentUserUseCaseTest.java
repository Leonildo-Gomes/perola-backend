package cv.perola.modules.identity.usecase.user;

import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.UpdateUserProfileRequest;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import cv.perola.modules.identity.validator.user.UpdateUserProfileValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCurrentUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UpdateUserProfileValidator updateUserProfileValidator;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UpdateCurrentUserUseCase updateCurrentUserUseCase;

    @Test
    @DisplayName("Deve atualizar nome, telefone e avatarUrl do utilizador autenticado")
    void shouldUpdateCurrentUserProfileAndAvatarUrl() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .name("Carlos")
                .email("carlos@perola.cv")
                .phone("+2389911111")
                .isActive(true)
                .build();
        user.setId(userId);

        String avatarUrl = "http://localhost:8080/api/v1/media/users/" + userId + "/avatar/pic.webp";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Carlos Silva",
                "+2389922222",
                avatarUrl
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse expected = new UserProfileResponse(
                userId,
                "Carlos Silva",
                "carlos@perola.cv",
                "+2389922222",
                avatarUrl,
                List.of(UserRole.CUSTOMER),
                List.of()
        );
        when(tokenService.buildUserProfile(user)).thenReturn(expected);

        // Act
        UserProfileResponse response = updateCurrentUserUseCase.execute(userId, request);

        // Assert
        verify(updateUserProfileValidator).validate(user, request);
        assertThat(user.getName()).isEqualTo("Carlos Silva");
        assertThat(user.getPhone()).isEqualTo("+2389922222");
        assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(response).isEqualTo(expected);
    }
}

