package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TimeOffMapper {

    public EmployeeTimeOffEntity toEntity(UUID employeeId, CreateTimeOffRequest request) {
        boolean isAllDay = request.resolvedIsAllDay();
        return EmployeeTimeOffEntity.builder()
                .employeeId(employeeId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isAllDay(isAllDay)
                .startTime(isAllDay ? null : request.startTime())
                .endTime(isAllDay ? null : request.endTime())
                .type(request.type())
                .note(request.note())
                .build();
    }

    public TimeOffResponse toResponse(EmployeeTimeOffEntity entity) {
        return new TimeOffResponse(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isAllDay(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getType(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
