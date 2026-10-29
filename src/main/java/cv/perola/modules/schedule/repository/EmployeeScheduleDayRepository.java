package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeScheduleDayRepository extends JpaRepository<EmployeeScheduleDayEntity, UUID> {

    List<EmployeeScheduleDayEntity> findAllByEmployeeScheduleIdOrderByDayOfWeekAsc(UUID employeeScheduleId);

    List<EmployeeScheduleDayEntity> findAllByEmployeeScheduleIdIn(Collection<UUID> employeeScheduleIds);

    Optional<EmployeeScheduleDayEntity> findByEmployeeScheduleIdAndDayOfWeek(UUID employeeScheduleId, DayOfWeek dayOfWeek);

    void deleteAllByEmployeeScheduleId(UUID employeeScheduleId);
}

