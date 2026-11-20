package cv.perola.modules.booking.repository;

import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessCustomerRepository extends
        JpaRepository<BusinessCustomerEntity, UUID>,
        JpaSpecificationExecutor<BusinessCustomerEntity> {

    Optional<BusinessCustomerEntity> findByBusinessIdAndCustomerId(UUID businessId, UUID customerId);

    List<BusinessCustomerEntity> findByBusinessIdAndCustomerIdIn(UUID businessId, Collection<UUID> customerIds);

    boolean existsByBusinessIdAndCustomerId(UUID businessId, UUID customerId);

    List<BusinessCustomerEntity> findAllByBusinessId(UUID businessId);

    Page<BusinessCustomerEntity> findAllByBusinessId(UUID businessId, Pageable pageable);

    Page<BusinessCustomerEntity> findAllByBusinessIdAndTier(UUID businessId, CustomerTier tier, Pageable pageable);

    default Page<BusinessCustomerEntity> findBusinessCustomers(
            UUID businessId,
            CustomerTier tier,
            Collection<UUID> customerIdsFilter,
            Pageable pageable
    ) {
        Specification<BusinessCustomerEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("businessId"), businessId));
            if (tier != null) {
                predicates.add(cb.equal(root.get("tier"), tier));
            }
            if (customerIdsFilter != null) {
                predicates.add(root.get("customerId").in(customerIdsFilter));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(spec, pageable);
    }
}
