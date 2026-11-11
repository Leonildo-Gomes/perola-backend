package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - EmployeeMapper")
class EmployeeMapperTest {

    private final EmployeeMapper employeeMapper = new EmployeeMapper();

    @Test
    @DisplayName("Should map parameters to EmployeeEntity correctly")
    void shouldMapParametersToEmployeeEntity() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String displayName = "Teresa Lopes";

        // Act
        EmployeeEntity entity = employeeMapper.toEntity(businessId, userId, displayName);

        // Assert
        assertNotNull(entity);
        assertEquals(businessId, entity.getBusinessId());
        assertEquals(userId, entity.getUserId());
        assertEquals("Teresa Lopes", entity.getDisplayName());
        assertNull(entity.getSpecialty());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("Should trim display name when mapping to EmployeeEntity")
    void shouldTrimDisplayName() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String displayName = "  João Silva  ";

        // Act
        EmployeeEntity entity = employeeMapper.toEntity(businessId, userId, displayName);

        // Assert
        assertNotNull(entity);
        assertEquals("João Silva", entity.getDisplayName());
    }

    @Test
    @DisplayName("Should map CreateEmployeeRequest with trimmed specialty to EmployeeEntity")
    void shouldMapCreateEmployeeRequestWithSpecialty() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                userId,
                "  Helena Silva  ",
                "  Cabeleireira Sénior & Trancista  "
        );

        // Act
        EmployeeEntity entity = employeeMapper.toEntity(request, businessId);

        // Assert
        assertNotNull(entity);
        assertEquals(businessId, entity.getBusinessId());
        assertEquals(userId, entity.getUserId());
        assertEquals("Helena Silva", entity.getDisplayName());
        assertEquals("Cabeleireira Sénior & Trancista", entity.getSpecialty());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("Should convert blank specialty to null when mapping CreateEmployeeRequest")
    void shouldConvertBlankSpecialtyToNull() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(null, "Carlos Lima", "   ");

        // Act
        EmployeeEntity entity = employeeMapper.toEntity(request, businessId);

        // Assert
        assertNotNull(entity);
        assertNull(entity.getSpecialty());
    }

    @Test
    @DisplayName("Should map EmployeeEntity with specialty to EmployeeResponse")
    void shouldMapEmployeeEntityToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        EmployeeEntity entity = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(userId)
                .displayName("Helena Silva")
                .specialty("Cabeleireira Sénior & Trancista")
                .isActive(true)
                .build();
        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Act
        EmployeeResponse response = employeeMapper.toResponse(entity);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(businessId, response.businessId());
        assertEquals(userId, response.userId());
        assertEquals("Helena Silva", response.displayName());
        assertEquals("Cabeleireira Sénior & Trancista", response.specialty());
        assertTrue(response.isActive());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
}
