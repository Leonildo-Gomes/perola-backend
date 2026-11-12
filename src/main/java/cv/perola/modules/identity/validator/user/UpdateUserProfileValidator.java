package cv.perola.modules.identity.validator.user;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.UpdateUserProfileRequest;
import cv.perola.modules.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateUserProfileValidator {

    private final UserRepository userRepository;

    public void validate(UserEntity user, UpdateUserProfileRequest request) {
        if (request == null) {
            throw new ValidationException("Os dados de atualização do perfil são obrigatórios.");
        }

        if (request.name() != null && request.name().isBlank()) {
            throw new ValidationException("O nome do utilizador não pode ficar em branco.");
        }

        if (request.phone() != null) {
            String trimmedPhone = request.phone().trim();
            if (trimmedPhone.isEmpty()) {
                throw new ValidationException("O telefone não pode ficar em branco.");
            }
            if (!trimmedPhone.equals(user.getPhone()) && userRepository.existsByPhone(trimmedPhone)) {
                throw new ConflictException("O telefone '" + trimmedPhone + "' já se encontra associado a outra conta.");
            }
        }
    }
}

