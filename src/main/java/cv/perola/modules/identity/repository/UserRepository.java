package cv.perola.modules.identity.repository;

import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.service.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
        SELECT new cv.perola.modules.identity.service.UserSummary(
            u.id, u.email, u.phone, u.avatarUrl
        )
        FROM UserEntity u
        WHERE u.id IN :userIds
    """)
    List<UserSummary> findSummariesByIdIn(@Param("userIds") Collection<UUID> userIds);
}
