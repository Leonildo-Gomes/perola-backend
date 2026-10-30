package cv.perola.modules.identity.validator.auth;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticateUserValidator {

    private final PasswordEncoder passwordEncoder;

    public void validate(UserEntity user, LoginRequest request) {
        if (!user.isActive()) {
            throw new AuthenticationFailedException("A sua conta encontra-se desativada.");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou palavra-passe incorretos.");
        }
    }
}

