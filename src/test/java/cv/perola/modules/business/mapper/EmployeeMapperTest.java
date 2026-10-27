package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.EmployeeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
