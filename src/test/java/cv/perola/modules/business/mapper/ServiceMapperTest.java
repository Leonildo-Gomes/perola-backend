package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceMapperTest {

    private final ServiceMapper serviceMapper = new ServiceMapper();

    @Test
    @DisplayName("Deve converter CreateServiceRequest para ServiceEntity com campos mapeados")
    void shouldConvertRequestToEntity() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest(
                "Corte e Barba",
                "Corte masculino tradicional com finalização de barba",
                45,
                new BigDecimal("1500.00")
        );

        // Act
        ServiceEntity entity = serviceMapper.toEntity(request, businessId);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getBusinessId()).isEqualTo(businessId);
        assertThat(entity.getName()).isEqualTo("Corte e Barba");
        assertThat(entity.getDescription()).isEqualTo("Corte masculino tradicional com finalização de barba");
        assertThat(entity.getDurationMinutes()).isEqualTo(45);
        assertThat(entity.getPrice()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve converter ServiceEntity para ServiceResponse com sucesso")
    void shouldConvertEntityToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        Instant now = Instant.now();
        ServiceEntity entity = ServiceEntity.builder()
                .businessId(businessId)
                .name("Manicure & Pedicure")
                .description("Unhas completas com esmaltação em gel")
                .durationMinutes(60)
                .price(new BigDecimal("2000.00"))
                .isActive(true)
                .build();
        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Act
        ServiceResponse response = serviceMapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.businessId()).isEqualTo(businessId);
        assertThat(response.name()).isEqualTo("Manicure & Pedicure");
        assertThat(response.description()).isEqualTo("Unhas completas com esmaltação em gel");
        assertThat(response.durationMinutes()).isEqualTo(60);
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null quando entidade for nula")
    void shouldReturnNullWhenEntityIsNull() {
        // Arrange & Act
        ServiceResponse response = serviceMapper.toResponse(null);

        // Assert
        assertThat(response).isNull();
    }
}
