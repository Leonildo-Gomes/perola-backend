package cv.perola.modules.identity.mapper;

import cv.perola.core.security.TenantMembership;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.dto.UserMembershipResponse;
import cv.perola.modules.identity.dto.UserProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - UserMapper")
class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    @DisplayName("Should map UserEntity with roles and memberships to UserProfileResponse")
    void shouldMapUserEntityWithRolesAndMembershipsToResponse() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .name("Maria Silva")
                .email("maria@perola.cv")
                .phone("+2389912233")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();
        user.setId(userId);
        List<UserRole> roles = List.of(UserRole.OWNER, UserRole.CUSTOMER);
        List<TenantMembership> memberships = List.of(new TenantMembership(businessId, "OWNER"));

        // Act
        UserProfileResponse response = userMapper.toResponse(user, roles, memberships);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("Maria Silva", response.name());
        assertEquals("maria@perola.cv", response.email());
        assertEquals("+2389912233", response.phone());
        assertEquals("https://example.com/avatar.jpg", response.avatarUrl());
        assertEquals(2, response.roles().size());
        assertTrue(response.roles().contains(UserRole.OWNER));
        assertTrue(response.roles().contains(UserRole.CUSTOMER));
        assertEquals(1, response.businesses().size());
        assertEquals(businessId, response.businesses().getFirst().businessId());
        assertEquals(UserRole.OWNER, response.businesses().getFirst().role());
    }

    @Test
    @DisplayName("Should map UserEntity with null roles and null memberships gracefully")
    void shouldMapUserEntityWithNullCollectionsGracefully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .name("Carlos Santos")
                .email("carlos@perola.cv")
                .build();
        user.setId(userId);

        // Act
        UserProfileResponse response = userMapper.toResponse(user, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("Carlos Santos", response.name());
        assertNotNull(response.roles());
        assertTrue(response.roles().isEmpty());
        assertNotNull(response.businesses());
        assertTrue(response.businesses().isEmpty());
    }

    @Test
    @DisplayName("Should return null when mapping null UserEntity")
    void shouldReturnNullWhenUserEntityIsNull() {
        // Arrange & Act
        UserProfileResponse response = userMapper.toResponse(null, List.of(UserRole.CUSTOMER), null);

        // Assert
        assertNull(response);
    }

    @Test
    @DisplayName("Should correctly map TenantMembership to UserMembershipResponse")
    void shouldMapTenantMembershipToResponse() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(businessId, "EMPLOYEE");

        // Act
        UserMembershipResponse response = userMapper.toResponse(membership);

        // Assert
        assertNotNull(response);
        assertEquals(businessId, response.businessId());
        assertEquals(UserRole.EMPLOYEE, response.role());
    }

    @Test
    @DisplayName("Should return null when mapping null TenantMembership")
    void shouldReturnNullWhenTenantMembershipIsNull() {
        // Arrange & Act
        UserMembershipResponse response = userMapper.toResponse((TenantMembership) null);

        // Assert
        assertNull(response);
    }

    @Test
    @DisplayName("Should handle invalid role name in TenantMembership by setting null role")
    void shouldHandleInvalidRoleInTenantMembership() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(businessId, "INVALID_ROLE");

        // Act
        UserMembershipResponse response = userMapper.toResponse(membership);

        // Assert
        assertNotNull(response);
        assertEquals(businessId, response.businessId());
        assertNull(response.role());
    }

    @Test
    @DisplayName("Should map RegisterRequest to UserEntity correctly")
    void shouldMapRegisterRequestToUserEntity() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Ana Paula",
                "ANA@perola.cv",
                "+2389914455",
                "Password123!"
        );
        String passwordHash = "$2a$10$hashedPasswordExample";

        // Act
        UserEntity entity = userMapper.toEntity(request, passwordHash);

        // Assert
        assertNotNull(entity);
        assertEquals("Ana Paula", entity.getName());
        assertEquals("ana@perola.cv", entity.getEmail());
        assertEquals("+2389914455", entity.getPhone());
        assertEquals(passwordHash, entity.getPasswordHash());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("Should return null when RegisterRequest is null")
    void shouldReturnNullWhenRegisterRequestIsNull() {
        // Arrange & Act
        UserEntity entity = userMapper.toEntity(null, "hash");

        // Assert
        assertNull(entity);
    }

    @Test
    @DisplayName("Should map Google OAuth data to UserEntity correctly")
    void shouldMapGoogleOAuthDataToUserEntity() {
        // Arrange
        String name = "Google User";
        String email = "GOOGLE.USER@GMAIL.COM";
        String googleId = "google-sub-123";
        String avatarUrl = "https://lh3.googleusercontent.com/photo.jpg";

        // Act
        UserEntity entity = userMapper.toEntity(name, email, googleId, avatarUrl);

        // Assert
        assertNotNull(entity);
        assertEquals("Google User", entity.getName());
        assertEquals("google.user@gmail.com", entity.getEmail());
        assertEquals("google-sub-123", entity.getGoogleId());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", entity.getAvatarUrl());
        assertEquals("", entity.getPhone());
        assertTrue(entity.isActive());
    }
}
