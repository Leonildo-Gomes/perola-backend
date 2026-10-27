package cv.perola.modules.schedule.repository;

import cv.perola.modules.schedule.domain.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<HolidayEntity, UUID> {

    List<HolidayEntity> findAllByOrderByHolidayDateAsc();

    @Query("SELECT h FROM HolidayEntity h WHERE EXTRACT(YEAR FROM h.holidayDate) = :year ORDER BY h.holidayDate ASC")
    List<HolidayEntity> findAllByYear(@Param("year") int year);

    Optional<HolidayEntity> findByHolidayDate(LocalDate holidayDate);
}
