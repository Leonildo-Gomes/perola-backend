package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.BusinessEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID> {

    Optional<BusinessEntity> findByOwnerId(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    Optional<BusinessEntity> findByIdAndIsActiveTrue(UUID id);

    @Query("""
        SELECT b FROM BusinessEntity b
        WHERE b.isActive = true
          AND (CAST(:search AS string) IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(b.location) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:island AS string) IS NULL OR LOWER(b.island) = LOWER(CAST(:island AS string)))
          AND (CAST(:city AS string) IS NULL OR LOWER(b.city) = LOWER(CAST(:city AS string)))
    """)
    Page<BusinessEntity> searchMarketplaceBusinesses(
            @Param("search") String search,
            @Param("island") String island,
            @Param("city") String city,
            Pageable pageable
    );
}

