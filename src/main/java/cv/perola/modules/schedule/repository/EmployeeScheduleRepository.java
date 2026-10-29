package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeScheduleRepository extends JpaRepository<EmployeeScheduleEntity, UUID> {

    List<EmployeeScheduleEntity> findAllByEmployeeIdOrderByIsDefaultDescCreatedAtAsc(UUID employeeId);

    List<EmployeeScheduleEntity> findAllByEmployeeIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtAsc(UUID employeeId);

    Optional<EmployeeScheduleEntity> findByIdAndEmployeeId(UUID id, UUID employeeId);

    Optional<EmployeeScheduleEntity> findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(UUID employeeId);
}
