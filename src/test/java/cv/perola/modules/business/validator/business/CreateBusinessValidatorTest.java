package cv.perola.modules.business.validator.business;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.exception.ConflictException;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.repository.BusinessRepository;
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
class CreateBusinessValidatorTest {

    @Mock
    private BusinessRepository businessRepository;

    @InjectMocks
    private CreateBusinessValidator validator;

    private CreateBusinessRequest buildSampleRequest() {
        return new CreateBusinessRequest(
                "Salão Beleza Pura",
                "Praia",
                "Santiago",
                "Plateau, Rua Pedonal",
                "+2389912345",
                "Atlantic/Cape_Verde"
        );
    }

    @Test
    @DisplayName("Deve passar a validação com sucesso quando o utilizador é válido e não possui salão registado")
    void shouldValidateSuccessfullyWhenUserHasNoBusiness() {
        UUID userId = UUID.randomUUID();
        CreateBusinessRequest request = buildSampleRequest();

        when(businessRepository.existsByOwnerId(userId)).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(userId, request));

        verify(businessRepository).existsByOwnerId(userId);
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException quando currentUserId for nulo")
    void shouldThrowAuthenticationFailedExceptionWhenUserIdIsNull() {
        CreateBusinessRequest request = buildSampleRequest();

        assertThrows(AuthenticationFailedException.class, () -> validator.validate(null, request));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o utilizador já for proprietário de um salão")
    void shouldThrowConflictExceptionWhenUserAlreadyHasBusiness() {
        UUID userId = UUID.randomUUID();
        CreateBusinessRequest request = buildSampleRequest();

        when(businessRepository.existsByOwnerId(userId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(userId, request));

        verify(businessRepository).existsByOwnerId(userId);
    }
}

