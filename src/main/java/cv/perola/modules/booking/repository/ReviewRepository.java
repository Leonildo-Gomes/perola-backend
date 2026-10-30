package cv.perola.modules.booking.repository;

import cv.perola.modules.booking.domain.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    boolean existsByAppointmentId(UUID appointmentId);

    Optional<ReviewEntity> findByAppointmentId(UUID appointmentId);

    Page<ReviewEntity> findAllByEmployeeIdOrderByCreatedAtDesc(UUID employeeId, Pageable pageable);

    @Query("""
            SELECT r FROM ReviewEntity r
            JOIN AppointmentEntity a ON a.id = r.appointmentId
            WHERE a.businessId = :businessId
            ORDER BY r.createdAt DESC
            """)
    Page<ReviewEntity> findAllByBusinessId(@Param("businessId") UUID businessId, Pageable pageable);
}

