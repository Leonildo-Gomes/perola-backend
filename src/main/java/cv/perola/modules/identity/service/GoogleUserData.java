package cv.perola.modules.identity.service;

public record GoogleUserData(
        String googleId,
        String email,
        String name,
        String avatarUrl
) {}
