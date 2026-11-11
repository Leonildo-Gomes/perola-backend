package cv.perola.modules.identity.service;

import cv.perola.modules.identity.domain.enums.UserRole;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountService {

    void assignRole(UUID userId, UserRole role);

    Optional<String> findUserNameById(UUID userId);

    Map<UUID, UserSummary> findUserSummariesByIds(Collection<UUID> userIds);

    boolean existsById(UUID userId);
}
