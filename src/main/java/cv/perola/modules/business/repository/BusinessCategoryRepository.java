package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessCategoryId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessCategoryRepository extends JpaRepository<BusinessCategoryEntity, BusinessCategoryId> {

    @EntityGraph(attributePaths = "category")
    List<BusinessCategoryEntity> findAllByBusinessId(UUID businessId);

    @EntityGraph(attributePaths = "category")
    List<BusinessCategoryEntity> findAllByBusinessIdIn(Collection<UUID> businessIds);

    void deleteAllByBusinessId(UUID businessId);
}

