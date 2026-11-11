package cv.perola.modules.business.validator.business;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.exception.ConflictException;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBusinessValidatorTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessCategoryValidator businessCategoryValidator;

    @InjectMocks
    private CreateBusinessValidator validator;

    private CreateBusinessRequest buildSampleRequest(UUID catId) {
        return new CreateBusinessRequest(
                "Salão Beleza Pura",
                "Praia",
                "Santiago",
                "Plateau, Rua Pedonal",
                "+2389912345",
                "Atlantic/Cape_Verde",
                List.of(new BusinessCategoryItemRequest(catId, true)));
    }

    @Test
    @DisplayName("Deve passar a validação com sucesso quando o utilizador é válido, não possui salão e categorias são válidas")
    void shouldValidateSuccessfullyWhenUserHasNoBusiness() {
        UUID userId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CreateBusinessRequest request = buildSampleRequest(catId);
        CategoryEntity category = CategoryEntity.builder().slug("hair").name("Cabeleireiro").build();
        category.setId(catId);

        when(businessRepository.existsByOwnerId(userId)).thenReturn(false);
        when(businessCategoryValidator.validateAndResolveCategories(request.categories()))
                .thenReturn(Map.of(catId, category));

        Map<UUID, CategoryEntity> result = validator.validate(userId, request);

        assertEquals(1, result.size());
        verify(businessRepository).existsByOwnerId(userId);
        verify(businessCategoryValidator).validateAndResolveCategories(request.categories());
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException quando currentUserId for nulo")
    void shouldThrowAuthenticationFailedExceptionWhenUserIdIsNull() {
        CreateBusinessRequest request = buildSampleRequest(UUID.randomUUID());

        assertThrows(AuthenticationFailedException.class, () -> validator.validate(null, request));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o utilizador já for proprietário de um salão")
    void shouldThrowConflictExceptionWhenUserAlreadyHasBusiness() {
        UUID userId = UUID.randomUUID();
        CreateBusinessRequest request = buildSampleRequest(UUID.randomUUID());

        when(businessRepository.existsByOwnerId(userId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validate(userId, request));

        verify(businessRepository).existsByOwnerId(userId);
    }
}