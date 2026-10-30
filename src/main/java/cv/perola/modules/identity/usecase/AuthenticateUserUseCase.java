package cv.perola.modules.identity.usecase;

import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import cv.perola.modules.identity.validator.auth.AuthenticateUserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthenticateUserValidator authenticateUserValidator;

    @Transactional
    public AuthResponse execute(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Email ou palavra-passe incorretos."));

        authenticateUserValidator.validate(user, request);

        return tokenService.issueTokens(user);
    }
}
