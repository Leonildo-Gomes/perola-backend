package cv.perola.modules.identity.usecase;

import cv.perola.core.exception.ConflictException;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.mapper.UserMapper;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import cv.perola.modules.identity.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("O email '" + request.email() + "' já se encontra registado.");
        }

        if (userRepository.existsByPhone(request.phone().trim())) {
            throw new ConflictException("O telefone '" + request.phone() + "' já se encontra associado a outra conta.");
        }

        UserEntity user = userMapper.toEntity(request, passwordEncoder.encode(request.password()));
        UserEntity savedUser = userRepository.save(user);

        UserRoleEntity defaultRole = UserRoleEntity.builder()
                .userId(savedUser.getId())
                .role(UserRole.CUSTOMER)
                .build();
        userRoleRepository.save(defaultRole);

        return tokenService.issueTokens(savedUser);
    }
}
