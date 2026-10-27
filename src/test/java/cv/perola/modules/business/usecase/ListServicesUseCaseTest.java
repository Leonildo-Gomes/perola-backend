package cv.perola.modules.business.usecase;

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
class ListServicesUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private ListServicesUseCase listServicesUseCase;

    @Test
    @DisplayName("Deve listar todos os serviços ativos de um salão")
    void shouldListActiveServicesForBusiness() {
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

        ServiceEntity s2 = ServiceEntity.builder()
                .businessId(businessId)
                .name("Barba")
                .durationMinutes(20)
                .price(new BigDecimal("800.00"))
                .isActive(true)
                .build();
        s2.setId(UUID.randomUUID());
        s2.setCreatedAt(Instant.now());
        s2.setUpdatedAt(Instant.now());

        when(serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of(s1, s2));

        // Act
        List<ServiceResponse> result = listServicesUseCase.execute(businessId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Corte Masculino");
        assertThat(result.get(1).name()).isEqualTo("Barba");

        verify(serviceRepository).findAllByBusinessIdAndIsActiveTrue(businessId);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o salão não possui serviços cadastrados")
    void shouldReturnEmptyListWhenNoServicesExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        when(serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of());

        // Act
        List<ServiceResponse> result = listServicesUseCase.execute(businessId);

        // Assert
        assertThat(result).isEmpty();

        verify(serviceRepository).findAllByBusinessIdAndIsActiveTrue(businessId);
    }
}
