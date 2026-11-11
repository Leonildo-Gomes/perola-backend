package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleBusinessStatusUseCaseTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessCategoryRepository businessCategoryRepository;

    @Mock
    private BusinessMapper businessMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private ToggleBusinessStatusUseCase useCase;

    @Test
    @DisplayName("Deve alternar status de ativo (true) para inativo (false)")
    void shouldToggleFromActiveToInactive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        BusinessEntity business = BusinessEntity.builder()
                .ownerId(UUID.randomUUID())
                .name("Salão Pérola")
                .island("Santiago")
                .city("Praia")
                .location("Plateau")
                .contactPhone("9911223")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();

        BusinessResponse expectedResponse = new BusinessResponse(
                businessId, business.getOwnerId(), "Salão Pérola", "Santiago", "Praia",
                "Plateau", "9911223", "Atlantic/Cape_Verde", List.of(), false, Instant.now(), Instant.now());

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessCategoryRepository.findAllByBusinessId(businessId)).thenReturn(List.of());
        when(categoryMapper.toBusinessCategoryResponses(anyList())).thenReturn(List.of());
        when(businessMapper.toResponse(any(BusinessEntity.class), anyList())).thenReturn(expectedResponse);

        // Act
        BusinessResponse response = useCase.execute(businessId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();
        assertThat(business.isActive()).isFalse();
        verify(businessRepository).findById(businessId);
        verify(businessRepository).save(business);
    }

    @Test
    @DisplayName("Deve alternar status de inativo (false) para ativo (true)")
    void shouldToggleFromInactiveToActive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        BusinessEntity business = BusinessEntity.builder()
                .ownerId(UUID.randomUUID())
                .name("Salão Pérola")
                .island("Santiago")
                .city("Praia")
                .location("Plateau")
                .contactPhone("9911223")
                .timezone("Atlantic/Cape_Verde")
                .isActive(false)
                .build();

        BusinessResponse expectedResponse = new BusinessResponse(
                businessId, business.getOwnerId(), "Salão Pérola", "Santiago", "Praia",
                "Plateau", "9911223", "Atlantic/Cape_Verde", List.of(), true, Instant.now(), Instant.now()
        );

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessCategoryRepository.findAllByBusinessId(businessId)).thenReturn(List.of());
        when(categoryMapper.toBusinessCategoryResponses(anyList())).thenReturn(List.of());
        when(businessMapper.toResponse(any(BusinessEntity.class), anyList())).thenReturn(expectedResponse);

        // Act
        BusinessResponse response = useCase.execute(businessId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isTrue();
        assertThat(business.isActive()).isTrue();
        verify(businessRepository).findById(businessId);
        verify(businessRepository).save(business);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando salão não for encontrado")
    void shouldThrowWhenBusinessNotFound() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(businessId));
        verify(businessRepository).findById(businessId);
    }
    }
