package cv.perola.modules.identity.validator.user;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.UpdateUserProfileRequest;
import cv.perola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserProfileValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateUserProfileValidator validator;

    @Test
    @DisplayName("Deve validar atualização de perfil com sucesso quando dados são válidos")
    void shouldValidateSuccessfullyWhenRequestIsValid() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .name("Carlos")
                .phone("+2389911111")
                .build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Carlos Silva",
                "+2389922222",
                "http://localhost:8080/api/v1/media/users/123/avatar/a.webp"
        );
        when(userRepository.existsByPhone("+2389922222")).thenReturn(false);

        // Act & Assert
        assertThatCode(() -> validator.validate(user, request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar atualização quando telefone já pertence a outro utilizador")
    void shouldThrowConflictExceptionWhenPhoneAlreadyExists() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .name("Carlos")
                .phone("+2389911111")
                .build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, "+2389922222", null);
        when(userRepository.existsByPhone("+2389922222")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(user, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Deve rejeitar nome em branco com ValidationException")
    void shouldThrowValidationExceptionWhenNameIsBlank() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .name("Carlos")
                .phone("+2389911111")
                .build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("   ", null, null);

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(user, request))
                .isInstanceOf(ValidationException.class);
    }
}

