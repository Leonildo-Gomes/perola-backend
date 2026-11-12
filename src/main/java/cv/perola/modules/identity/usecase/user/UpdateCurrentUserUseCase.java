package cv.perola.modules.identity.usecase.user;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.UpdateUserProfileRequest;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import cv.perola.modules.identity.validator.user.UpdateUserProfileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCurrentUserUseCase {

    private final UserRepository userRepository;
    private final UpdateUserProfileValidator updateUserProfileValidator;
    private final TokenService tokenService;

    @Transactional
    public UserProfileResponse execute(UUID userId, UpdateUserProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        updateUserProfileValidator.validate(user, request);

        if (request.name() != null) {
            user.setName(request.name().trim());
        }

        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }

        if (request.avatarUrl() != null) {
            String trimmedAvatar = request.avatarUrl().trim();
            user.setAvatarUrl(trimmedAvatar.isEmpty() ? null : trimmedAvatar);
        }

        UserEntity saved = userRepository.save(user);
        return tokenService.buildUserProfile(saved);
    }
}

