package cv.perola.modules.identity.dto;

import cv.perola.modules.identity.domain.enums.UserRole;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String avatarUrl,
        List<UserRole> roles,
        List<UserMembershipResponse> businesses
) {}
