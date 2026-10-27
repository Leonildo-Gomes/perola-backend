package cv.perola.modules.identity.mapper;

import cv.perola.core.security.TenantMembership;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.dto.UserMembershipResponse;
import cv.perola.modules.identity.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserMapper {

    public UserEntity toEntity(RegisterRequest request, String passwordHash) {
        if (request == null) {
            return null;
        }
        return UserEntity.builder()
                .name(request.name() != null ? request.name().trim() : null)
                .email(request.email() != null ? request.email().trim().toLowerCase() : null)
                .phone(request.phone() != null ? request.phone().trim() : null)
                .passwordHash(passwordHash)
                .isActive(true)
                .build();
    }

    public UserEntity toEntity(String name, String email, String googleId, String avatarUrl) {
        return UserEntity.builder()
                .name(name != null ? name.trim() : null)
                .email(email != null ? email.trim().toLowerCase() : null)
                .phone("")
                .googleId(googleId)
                .avatarUrl(avatarUrl)
                .isActive(true)
                .build();
    }

    public UserProfileResponse toResponse(
            UserEntity user,
            List<UserRole> roles,
            List<TenantMembership> memberships
    ) {
        if (user == null) {
            return null;
        }

        List<UserMembershipResponse> businessMemberships = memberships != null
                ? memberships.stream()
                .map(this::toResponse)
                .toList()
                : Collections.emptyList();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                roles != null ? List.copyOf(roles) : Collections.emptyList(),
                businessMemberships
        );
    }

    public UserProfileResponse toResponse(UserEntity user, List<UserRole> roles) {
        return toResponse(user, roles, Collections.emptyList());
    }

    public UserMembershipResponse toResponse(TenantMembership membership) {
        if (membership == null) {
            return null;
        }
        return new UserMembershipResponse(
                membership.businessId(),
                parseUserRoleOrNull(membership.role())
        );
    }

    private UserRole parseUserRoleOrNull(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(roleName.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
