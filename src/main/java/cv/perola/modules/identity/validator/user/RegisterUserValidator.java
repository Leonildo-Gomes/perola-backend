package cv.perola.modules.identity.validator.user;

import cv.perola.core.exception.ConflictException;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterUserValidator {

    private final UserRepository userRepository;

    public void validate(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("O email '" + request.email() + "' já se encontra registado.");
        }

        if (userRepository.existsByPhone(request.phone().trim())) {
            throw new ConflictException("O telefone '" + request.phone() + "' já se encontra associado a outra conta.");
        }
    }
}

