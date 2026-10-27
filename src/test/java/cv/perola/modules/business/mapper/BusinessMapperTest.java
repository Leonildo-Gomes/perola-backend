package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - BusinessMapper")
class BusinessMapperTest {

    private final BusinessMapper businessMapper = new BusinessMapper();

    @Test
    @DisplayName("Should correctly map BusinessEntity to BusinessResponse")
    void shouldMapEntityToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        BusinessEntity entity = BusinessEntity.builder()
                .ownerId(ownerId)
                .name("Salão Beleza Crioula")
                .island("Santiago")
                .city("Praia")
                .location("Palmarejo, Rua 3")
                .contactPhone("+2389912345")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Act
        BusinessResponse response = businessMapper.toResponse(entity);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(ownerId, response.ownerId());
        assertEquals("Salão Beleza Crioula", response.name());
        assertEquals("Santiago", response.island());
        assertEquals("Praia", response.city());
        assertEquals("Palmarejo, Rua 3", response.location());
        assertEquals("+2389912345", response.contactPhone());
        assertEquals("Atlantic/Cape_Verde", response.timezone());
        assertTrue(response.isActive());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    @DisplayName("Should return null when mapping null BusinessEntity to response")
    void shouldReturnNullWhenEntityIsNull() {
        // Arrange & Act
        BusinessResponse response = businessMapper.toResponse(null);

        // Assert
        assertNull(response);
    }

    @Test
    @DisplayName("Should map CreateBusinessRequest to BusinessEntity with custom timezone")
    void shouldMapCreateRequestToEntity() {
        // Arrange
        UUID ownerId = UUID.randomUUID();
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Espaço Glamour",
                "São Vicente",
                "Mindelo",
                "Rua de Lisboa",
                "+2389918877",
                "Atlantic/Azores"
        );

        // Act
        BusinessEntity entity = businessMapper.toEntity(request, ownerId);

        // Assert
        assertNotNull(entity);
        assertEquals(ownerId, entity.getOwnerId());
        assertEquals("Espaço Glamour", entity.getName());
        assertEquals("São Vicente", entity.getIsland());
        assertEquals("Mindelo", entity.getCity());
        assertEquals("Rua de Lisboa", entity.getLocation());
        assertEquals("+2389918877", entity.getContactPhone());
        assertEquals("Atlantic/Azores", entity.getTimezone());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("Should default timezone to Atlantic/Cape_Verde when not provided in CreateBusinessRequest")
    void shouldDefaultTimezoneWhenNotProvided() {
        // Arrange
        UUID ownerId = UUID.randomUUID();
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Central",
                "Sal",
                "Santa Maria",
                "Pedonal",
                "+2389934455",
                null
        );

        // Act
        BusinessEntity entity = businessMapper.toEntity(request, ownerId);

        // Assert
        assertNotNull(entity);
        assertEquals("Atlantic/Cape_Verde", entity.getTimezone());
    }

    @Test
    @DisplayName("Should return null when CreateBusinessRequest is null")
    void shouldReturnNullWhenCreateRequestIsNull() {
        // Arrange & Act
        BusinessEntity entity = businessMapper.toEntity(null, UUID.randomUUID());

        // Assert
        assertNull(entity);
    }
}
