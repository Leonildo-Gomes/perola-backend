package cv.perola.modules.booking.repository;

import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends
        JpaRepository<AppointmentEntity, UUID>,
        JpaSpecificationExecutor<AppointmentEntity> {

    default Page<AppointmentEntity> findCustomerAppointments(
            UUID customerId,
            AppointmentStatus status,
            Pageable pageable) {
        Specification<AppointmentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), customerId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "startTime"));
        return findAll(spec, sortedPageable);
    }

    default Page<AppointmentEntity> findBusinessAppointments(
            UUID businessId,
            UUID employeeId,
            AppointmentStatus status,
            Instant startFrom,
            Instant startTo,
            Pageable pageable
    ) {
        Specification<AppointmentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("businessId"), businessId));
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employeeId"), employeeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), startFrom));
            }
            if (startTo != null) {
                predicates.add(cb.lessThan(root.get("startTime"), startTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "startTime")
        );
        return findAll(spec, sortedPageable);
    }
}
