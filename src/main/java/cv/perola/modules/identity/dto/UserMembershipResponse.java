package cv.perola.modules.identity.dto;

import cv.perola.modules.identity.domain.enums.UserRole;

import java.util.UUID;

public record UserMembershipResponse(
        UUID businessId,
        UserRole role
) {}
