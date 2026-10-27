package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.BreakItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class ScheduleMapper {

    public EmployeeScheduleEntity toEntity(UUID employeeId, WeeklyScheduleItemRequest request) {
        return EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .isActive(request.resolvedIsActive())
                .build();
    }

    public EmployeeScheduleBreakEntity toEntity(UUID employeeScheduleId, BreakItemRequest request) {
        return EmployeeScheduleBreakEntity.builder()
                .employeeScheduleId(employeeScheduleId)
                .title(request.title())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();
    }

    public BreakItemResponse toResponse(EmployeeScheduleBreakEntity entity) {
        return new BreakItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }

    public WeeklyScheduleItemResponse toResponse(EmployeeScheduleEntity entity, List<EmployeeScheduleBreakEntity> breaks) {
        List<BreakItemResponse> breakResponses = breaks != null
                ? breaks.stream().map(this::toResponse).toList()
                : Collections.emptyList();

        return new WeeklyScheduleItemResponse(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isActive(),
                breakResponses
        );
    }

    public WeeklyScheduleResponse toResponse(UUID employeeId, List<WeeklyScheduleItemResponse> schedules) {
        return new WeeklyScheduleResponse(
                employeeId,
                schedules != null ? schedules : Collections.emptyList()
        );
    }
}
