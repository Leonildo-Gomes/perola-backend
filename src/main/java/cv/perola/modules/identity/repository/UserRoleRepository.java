package cv.perola.modules.identity.repository;

import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.domain.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    List<UserRoleEntity> findAllByUserId(UUID userId);

    Optional<UserRoleEntity> findByUserIdAndRole(UUID userId, UserRole role);

    boolean existsByUserIdAndRole(UUID userId, UserRole role);

    void deleteAllByUserId(UUID userId);
}
