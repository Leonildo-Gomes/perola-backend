package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.ServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    List<ServiceEntity> findAllByBusinessId(UUID businessId);

    List<ServiceEntity> findAllByBusinessIdAndIsActiveTrue(UUID businessId);

    long countByBusinessIdAndIsActiveTrue(UUID businessId);

    Optional<ServiceEntity> findByIdAndBusinessId(UUID id, UUID businessId);

    Optional<ServiceEntity> findByIdAndBusinessIdAndIsActiveTrue(UUID id, UUID businessId);

    boolean existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue(String name, UUID businessId);

    List<ServiceEntity> findAllByBusinessIdAndIdIn(UUID businessId, Collection<UUID> ids);

    // Queries para busca no Marketplace público (BE-008)
    Page<ServiceEntity> findByIsActiveTrue(Pageable pageable);

    @Query("""
        SELECT s FROM ServiceEntity s
        JOIN BusinessEntity b ON b.id = s.businessId
        WHERE s.isActive = true
          AND b.isActive = true
          AND (CAST(:search AS string) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:island AS string) IS NULL OR LOWER(b.island) = LOWER(CAST(:island AS string)))
          AND (CAST(:city AS string) IS NULL OR LOWER(b.city) = LOWER(CAST(:city AS string)))
    """)
    Page<ServiceEntity> searchMarketplaceServices(
            @Param("search") String search,
            @Param("island") String island,
            @Param("city") String city,
            Pageable pageable
    );
}
