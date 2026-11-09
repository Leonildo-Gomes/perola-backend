package cv.perola.modules.business.usecase.service;

import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActiveServicesUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private ListActiveServicesUseCase listActiveServicesUseCase;

    @Test
    @DisplayName("Deve listar exclusivamente os serviços ativos do salão com sucesso")
    void shouldListActiveServicesSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        ServiceEntity s1 = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Masculino")
                .durationMinutes(30)
                .price(new BigDecimal("1000.00"))
                .isActive(true)
                .build();
        s1.setId(UUID.randomUUID());
        s1.setCreatedAt(Instant.now());
        s1.setUpdatedAt(Instant.now());

        when(serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of(s1));

        // Act
        List<ServiceResponse> result = listActiveServicesUseCase.execute(businessId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Corte Masculino");
        assertThat(result.get(0).isActive()).isTrue();

        verify(serviceRepository).findAllByBusinessIdAndIsActiveTrue(businessId);
    }
}

