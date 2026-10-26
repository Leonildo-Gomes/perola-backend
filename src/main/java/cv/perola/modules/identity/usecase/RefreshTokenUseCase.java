package cv.perola.modules.identity.usecase;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.exception.InvalidTokenException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.shared.HashUtils;
import cv.perola.modules.identity.domain.RefreshTokenEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.RefreshTokenRequest;
import cv.perola.modules.identity.repository.RefreshTokenRepository;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse execute(RefreshTokenRequest request) {
        String tokenHash = HashUtils.sha256(request.refreshToken().trim());

        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido ou não encontrado."));

        if (!tokenEntity.isValid()) {
            throw new InvalidTokenException("Refresh token expirado ou revogado.");
        }

        // Rotação atómica: revoga o refresh token anterior
        tokenEntity.setRevokedAt(Instant.now());
        refreshTokenRepository.save(tokenEntity);

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador associado ao token não encontrado."));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("A sua conta encontra-se desativada.");
        }

        return tokenService.issueTokens(user);
    }
}
