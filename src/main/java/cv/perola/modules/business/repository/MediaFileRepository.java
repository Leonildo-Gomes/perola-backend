package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.domain.enums.MediaScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFileEntity, UUID> {

    Optional<MediaFileEntity> findByStorageKey(String storageKey);

    List<MediaFileEntity> findAllByBusinessIdAndScopeOrderByCreatedAtDesc(UUID businessId, MediaScope scope);

    List<MediaFileEntity> findAllByUploadedByIdOrderByCreatedAtDesc(UUID uploadedById);
}

