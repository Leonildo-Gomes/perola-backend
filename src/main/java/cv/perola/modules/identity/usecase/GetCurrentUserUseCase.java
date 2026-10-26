package cv.perola.modules.identity.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public UserProfileResponse execute(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        return tokenService.buildUserProfile(user);
    }
}
