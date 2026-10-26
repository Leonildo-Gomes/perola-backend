package cv.perola.modules.identity.repository;

import cv.perola.modules.identity.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    void deleteAllByUserId(UUID userId);
}
