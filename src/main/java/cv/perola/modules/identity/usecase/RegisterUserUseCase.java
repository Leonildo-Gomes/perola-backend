package cv.perola.modules.identity.usecase;

import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.mapper.UserMapper;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import cv.perola.modules.identity.service.TokenService;
import cv.perola.modules.identity.validator.user.RegisterUserValidator;
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
    private final RegisterUserValidator registerUserValidator;

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        registerUserValidator.validate(request);

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
