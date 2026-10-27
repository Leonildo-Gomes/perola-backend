package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {

    Optional<EmployeeEntity> findByUserId(UUID userId);

    Optional<EmployeeEntity> findByBusinessIdAndUserId(UUID businessId, UUID userId);

    List<EmployeeEntity> findAllByBusinessId(UUID businessId);

    List<EmployeeEntity> findAllByBusinessIdAndIsActiveTrue(UUID businessId);

    boolean existsByUserId(UUID userId);

    boolean existsByBusinessIdAndUserId(UUID businessId, UUID userId);
}
