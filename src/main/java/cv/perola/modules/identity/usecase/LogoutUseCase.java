package cv.perola.modules.identity.usecase;

import cv.perola.core.shared.HashUtils;
import cv.perola.modules.identity.dto.RefreshTokenRequest;
import cv.perola.modules.identity.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void execute(RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = HashUtils.sha256(request.refreshToken().trim());
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        if (!token.isRevoked()) {
                            token.setRevokedAt(Instant.now());
                            refreshTokenRepository.save(token);
                        }
                    });
        }
    }
}
