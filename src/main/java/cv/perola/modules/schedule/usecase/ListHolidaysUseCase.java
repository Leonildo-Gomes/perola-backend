package cv.perola.modules.schedule.usecase;

import cv.perola.modules.schedule.domain.HolidayEntity;
import cv.perola.modules.schedule.dto.HolidayResponse;
import cv.perola.modules.schedule.mapper.HolidayMapper;
import cv.perola.modules.schedule.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListHolidaysUseCase {

    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;

    public List<HolidayResponse> execute(Integer year) {
        List<HolidayEntity> holidays = (year != null)
                ? holidayRepository.findAllByYear(year)
                : holidayRepository.findAllByOrderByHolidayDateAsc();

        return holidays.stream()
                .map(holidayMapper::toResponse)
                .toList();
    }
}
