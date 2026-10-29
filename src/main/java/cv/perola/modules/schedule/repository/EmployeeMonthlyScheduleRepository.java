package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeMonthlyScheduleRepository extends JpaRepository<EmployeeMonthlyScheduleEntity, UUID> {

    Optional<EmployeeMonthlyScheduleEntity> findByEmployeeIdAndYearAndMonth(UUID employeeId, int year, int month);

    List<EmployeeMonthlyScheduleEntity> findAllByEmployeeIdAndYearOrderByMonthAsc(UUID employeeId, int year);
}

