package cv.perola.modules.identity.service;

import cv.perola.core.security.JwtService;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserMembershipProvider;
import cv.perola.core.shared.HashUtils;
import cv.perola.modules.identity.domain.RefreshTokenEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.UserMembershipResponse;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.repository.RefreshTokenRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMembershipProvider userMembershipProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse issueTokens(UserEntity user) {
        List<UserRoleEntity> roleEntities = userRoleRepository.findAllByUserId(user.getId());
        List<UserRole> roles = roleEntities.stream()
                .map(UserRoleEntity::getRole)
                .toList();
        List<String> roleStrings = roles.stream()
                .map(UserRole::name)
                .toList();

        List<TenantMembership> memberships = userMembershipProvider.findMembershipsByUserId(user.getId());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roleStrings,
                memberships
        );

        String rawRefreshToken = generateAndSaveRefreshToken(user.getId());

        UserProfileResponse userProfile = buildUserProfile(user, roles, memberships);

        return AuthResponse.of(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpirationSeconds(),
                userProfile
        );
    }

    @Transactional
    public String generateAndSaveRefreshToken(UUID userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = HashUtils.sha256(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds());

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
        return rawToken;
    }

    public UserProfileResponse buildUserProfile(UserEntity user) {
        List<UserRoleEntity> roleEntities = userRoleRepository.findAllByUserId(user.getId());
        List<UserRole> roles = roleEntities.stream()
                .map(UserRoleEntity::getRole)
                .toList();
        List<TenantMembership> memberships = userMembershipProvider.findMembershipsByUserId(user.getId());
        return buildUserProfile(user, roles, memberships);
    }

    private UserProfileResponse buildUserProfile(
            UserEntity user,
            List<UserRole> roles,
            List<TenantMembership> memberships
    ) {
        List<UserMembershipResponse> businessMemberships = memberships != null
                ? memberships.stream()
                .map(m -> new UserMembershipResponse(
                        m.businessId(),
                        parseUserRoleOrNull(m.role())
                ))
                .toList()
                : Collections.emptyList();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                roles,
                businessMemberships
        );
    }

    private UserRole parseUserRoleOrNull(String roleName) {
        try {
            return roleName != null ? UserRole.valueOf(roleName) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
