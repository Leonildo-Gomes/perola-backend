package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class MonthlyScheduleMapper {

    public EmployeeMonthlyScheduleEntity toEntity(UUID employeeId, OpenMonthlyScheduleRequest request, Instant openedAt) {
        return EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(request.scheduleId())
                .year(request.year())
                .month(request.month())
                .isOpened(true)
                .openedAt(openedAt != null ? openedAt : Instant.now())
                .build();
    }

    public MonthlyScheduleResponse toResponse(EmployeeMonthlyScheduleEntity entity, String scheduleName) {
        return new MonthlyScheduleResponse(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getEmployeeScheduleId(),
                scheduleName,
                entity.getYear(),
                entity.getMonth(),
                entity.isOpened(),
                entity.getOpenedAt()
        );
    }
}

