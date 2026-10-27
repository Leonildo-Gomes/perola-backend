package cv.perola.modules.identity.service;

import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private DefaultUserAccountService userAccountService;

    @Test
    @DisplayName("Deve atribuir papel com sucesso quando o utilizador ainda não possui o papel")
    void shouldAssignRoleWhenNotAssignedYet() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserRole role = UserRole.OWNER;
        when(userRoleRepository.existsByUserIdAndRole(userId, role)).thenReturn(false);

        // Act
        userAccountService.assignRole(userId, role);

        // Assert
        ArgumentCaptor<UserRoleEntity> captor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleRepository).save(captor.capture());
        UserRoleEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRole()).isEqualTo(role);
    }

    @Test
    @DisplayName("Não deve persistir papel duplicado quando o utilizador já possui o papel")
    void shouldNotDuplicateRoleWhenAlreadyAssigned() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserRole role = UserRole.OWNER;
        when(userRoleRepository.existsByUserIdAndRole(userId, role)).thenReturn(true);

        // Act
        userAccountService.assignRole(userId, role);

        // Assert
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar o nome do utilizador quando ele existir no banco")
    void shouldReturnUserNameWhenUserExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .name("Maria Silva")
                .email("maria@example.com")
                .phone("+2389912345")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<String> userName = userAccountService.findUserNameById(userId);

        // Assert
        assertThat(userName).isPresent().contains("Maria Silva");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando o utilizador não existir")
    void shouldReturnEmptyOptionalWhenUserDoesNotExist() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        Optional<String> userName = userAccountService.findUserNameById(userId);

        // Assert
        assertThat(userName).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve verificar se o utilizador existe por ID")
    void shouldCheckIfUserExistsById() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        // Act
        boolean exists = userAccountService.existsById(userId);

        // Assert
        assertThat(exists).isTrue();
        verify(userRepository).existsById(userId);
    }
}
