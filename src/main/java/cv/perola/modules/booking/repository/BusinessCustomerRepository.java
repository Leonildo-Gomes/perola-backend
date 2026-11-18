package cv.perola.modules.booking.repository;

import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessCustomerRepository extends JpaRepository<BusinessCustomerEntity, UUID> {

    Optional<BusinessCustomerEntity> findByBusinessIdAndCustomerId(UUID businessId, UUID customerId);

    boolean existsByBusinessIdAndCustomerId(UUID businessId, UUID customerId);

    List<BusinessCustomerEntity> findAllByBusinessId(UUID businessId);

    Page<BusinessCustomerEntity> findAllByBusinessId(UUID businessId, Pageable pageable);

    Page<BusinessCustomerEntity> findAllByBusinessIdAndTier(UUID businessId, CustomerTier tier, Pageable pageable);
}
