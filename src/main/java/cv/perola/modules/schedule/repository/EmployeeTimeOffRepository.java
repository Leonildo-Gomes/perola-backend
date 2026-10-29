package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeTimeOffRepository extends JpaRepository<EmployeeTimeOffEntity, UUID> {

    List<EmployeeTimeOffEntity> findAllByEmployeeId(UUID employeeId);

    List<EmployeeTimeOffEntity> findAllByEmployeeIdOrderByStartDateAscStartTimeAsc(UUID employeeId);

    List<EmployeeTimeOffEntity> findAllByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID employeeId, LocalDate maxStartDate, LocalDate minEndDate
    );

    Optional<EmployeeTimeOffEntity> findByIdAndEmployeeId(UUID id, UUID employeeId);

    void deleteByIdAndEmployeeId(UUID id, UUID employeeId);

    void deleteAllByEmployeeIdAndTypeAndStartDateBetween(
            UUID employeeId, TimeOffType type, LocalDate startDate, LocalDate endDate
    );
}
