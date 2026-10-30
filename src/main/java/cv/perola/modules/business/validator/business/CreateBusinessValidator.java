package cv.perola.modules.business.validator.business;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.exception.ConflictException;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateBusinessValidator {

    private final BusinessRepository businessRepository;

    public void validate(UUID currentUserId, CreateBusinessRequest request) {
        if (currentUserId == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }

        if (businessRepository.existsByOwnerId(currentUserId)) {
            throw new ConflictException(
                    "O utilizador já possui um salão registado. No MVP é permitido apenas 1 negócio por proprietário.");
        }
    }
}

