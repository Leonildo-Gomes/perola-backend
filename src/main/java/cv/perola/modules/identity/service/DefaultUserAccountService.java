package cv.perola.modules.identity.service;

import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultUserAccountService implements UserAccountService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public void assignRole(UUID userId, UserRole role) {
        if (!userRoleRepository.existsByUserIdAndRole(userId, role)) {
            UserRoleEntity userRole = UserRoleEntity.builder()
                    .userId(userId)
                    .role(role)
                    .build();
            userRoleRepository.save(userRole);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findUserNameById(UUID userId) {
        return userRepository.findById(userId).map(UserEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> findUserSummariesByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<UUID> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findSummariesByIdIn(distinctIds)
                .stream()
                .collect(Collectors.toMap(UserSummary::id, Function.identity(), (first, second) -> first));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }
}
