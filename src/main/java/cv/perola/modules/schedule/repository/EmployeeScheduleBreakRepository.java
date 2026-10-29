package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeScheduleBreakRepository extends JpaRepository<EmployeeScheduleBreakEntity, UUID> {

    List<EmployeeScheduleBreakEntity> findAllByEmployeeScheduleDayId(UUID employeeScheduleDayId);

    List<EmployeeScheduleBreakEntity> findAllByEmployeeScheduleDayIdIn(Collection<UUID> employeeScheduleDayIds);

    void deleteAllByEmployeeScheduleDayIdIn(Collection<UUID> employeeScheduleDayIds);
}
