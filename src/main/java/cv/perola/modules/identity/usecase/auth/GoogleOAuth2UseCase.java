package cv.perola.modules.identity.usecase.auth;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.GoogleLoginRequest;
import cv.perola.modules.identity.mapper.UserMapper;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import cv.perola.modules.identity.service.GoogleTokenVerifier;
import cv.perola.modules.identity.service.GoogleUserData;
import cv.perola.modules.identity.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UseCase {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse execute(GoogleLoginRequest request) {
        GoogleUserData googleData = googleTokenVerifier.verify(request.idToken());
        String normalizedEmail = googleData.email().trim().toLowerCase();

        Optional<UserEntity> byGoogleId = userRepository.findByGoogleId(googleData.googleId());
        UserEntity user;

        if (byGoogleId.isPresent()) {
            user = byGoogleId.get();
            if (googleData.avatarUrl() != null && !googleData.avatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(googleData.avatarUrl());
                userRepository.save(user);
            }
        } else {
            Optional<UserEntity> byEmail = userRepository.findByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                user = byEmail.get();
                user.setGoogleId(googleData.googleId());
                if (googleData.avatarUrl() != null) {
                    user.setAvatarUrl(googleData.avatarUrl());
                }
                userRepository.save(user);
            } else {
                user = userMapper.toEntity(
                        googleData.name(),
                        normalizedEmail,
                        googleData.googleId(),
                        googleData.avatarUrl()
                );
                user = userRepository.save(user);

                UserRoleEntity role = UserRoleEntity.builder()
                        .userId(user.getId())
                        .role(UserRole.CUSTOMER)
                        .build();
                userRoleRepository.save(role);
            }
        }

        if (!user.isActive()) {
            throw new AuthenticationFailedException("A sua conta encontra-se desativada.");
        }

        return tokenService.issueTokens(user);
    }
}
