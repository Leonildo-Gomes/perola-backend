package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID> {

    Optional<BusinessEntity> findByOwnerId(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    Optional<BusinessEntity> findByIdAndIsActiveTrue(UUID id);
}
