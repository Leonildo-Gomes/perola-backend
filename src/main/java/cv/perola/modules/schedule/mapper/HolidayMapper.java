package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.HolidayEntity;
import cv.perola.modules.schedule.dto.HolidayResponse;
import org.springframework.stereotype.Component;

@Component
public class HolidayMapper {

    public HolidayResponse toResponse(HolidayEntity entity) {
        return new HolidayResponse(
                entity.getId(),
                entity.getHolidayDate(),
                entity.getName(),
                entity.isNational()
        );
    }
}
