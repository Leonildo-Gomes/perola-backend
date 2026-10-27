package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeScheduleRepository extends JpaRepository<EmployeeScheduleEntity, UUID> {

    List<EmployeeScheduleEntity> findAllByEmployeeId(UUID employeeId);

    List<EmployeeScheduleEntity> findAllByEmployeeIdOrderByDayOfWeekAsc(UUID employeeId);

    Optional<EmployeeScheduleEntity> findByEmployeeIdAndDayOfWeek(UUID employeeId, DayOfWeek dayOfWeek);

    void deleteAllByEmployeeId(UUID employeeId);
}
