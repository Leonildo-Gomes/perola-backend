package cv.perola.modules.identity.usecase;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse execute(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Email ou palavra-passe incorretos."));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("A sua conta encontra-se desativada.");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou palavra-passe incorretos.");
        }

        return tokenService.issueTokens(user);
    }
}
