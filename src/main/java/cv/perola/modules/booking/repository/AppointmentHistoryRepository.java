package cv.perola.modules.booking.repository;

import cv.perola.modules.booking.domain.AppointmentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistoryEntity, UUID> {

    List<AppointmentHistoryEntity> findAllByAppointmentIdOrderByCreatedAtAsc(UUID appointmentId);
}

