package cv.perola.modules.booking.repository;

import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends
        JpaRepository<AppointmentEntity, UUID>,
        JpaSpecificationExecutor<AppointmentEntity> {

    long countByBusinessIdAndCustomerIdAndStatusIn(
            UUID businessId,
            UUID customerId,
            java.util.Collection<AppointmentStatus> statuses);

    @Query("""
                SELECT a.customerId as customerId,
                       COUNT(a.id) as totalAppointments,
                       SUM(CASE WHEN CAST(a.status AS string) = 'COMPLETED' THEN 1L ELSE 0L END) as completedAppointments,
                       SUM(CASE WHEN CAST(a.status AS string) = 'CANCELLED' THEN 1L ELSE 0L END) as cancelledAppointments,
                       COALESCE(SUM(CASE WHEN CAST(a.status AS string) = 'COMPLETED' THEN a.servicePrice ELSE 0 END), 0) as totalSpentCve,
                       MAX(a.startTime) as lastAppointmentAt
                FROM AppointmentEntity a
                WHERE a.businessId = :businessId AND a.customerId IN :customerIds
                GROUP BY a.customerId
            """)

    List<CustomerAppointmentMetricsProjection> findMetricsForCustomers(
            @org.springframework.data.repository.query.Param("businessId") UUID businessId,
            @org.springframework.data.repository.query.Param("customerIds") java.util.Collection<UUID> customerIds);

    List<AppointmentEntity> findTop10ByBusinessIdAndCustomerIdOrderByStartTimeDesc(
            UUID businessId,
            UUID customerId);

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
            Pageable pageable) {
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
                Sort.by(Sort.Direction.ASC, "startTime"));
        return findAll(spec, sortedPageable);
    }
}