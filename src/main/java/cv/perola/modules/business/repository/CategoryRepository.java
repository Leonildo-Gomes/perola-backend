package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<CategoryEntity> findBySlug(String slug);

    Optional<CategoryEntity> findBySlugAndIsActiveTrue(String slug);

    Optional<CategoryEntity> findByIdAndIsActiveTrue(UUID id);

    List<CategoryEntity> findAllByIdInAndIsActiveTrue(Collection<UUID> ids);
}

