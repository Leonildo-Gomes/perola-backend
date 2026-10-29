package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.BreakItemResponse;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class ScheduleMapper {

    public EmployeeScheduleEntity toEntity(UUID employeeId, SaveEmployeeScheduleRequest request, boolean isDefault) {
        return EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name(request.name().trim())
                .isDefault(isDefault)
                .isActive(request.resolvedIsActive())
                .build();
    }

    public EmployeeScheduleDayEntity toEntity(UUID employeeScheduleId, ScheduleDayItemRequest request) {
        boolean isWorking = request.resolvedIsWorking();
        return EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(employeeScheduleId)
                .dayOfWeek(request.dayOfWeek())
                .isWorking(isWorking)
                .startTime(isWorking ? request.startTime() : null)
                .endTime(isWorking ? request.endTime() : null)
                .build();
    }

    public EmployeeScheduleBreakEntity toEntity(UUID employeeScheduleDayId, BreakItemRequest request) {
        return EmployeeScheduleBreakEntity.builder()
                .employeeScheduleDayId(employeeScheduleDayId)
                .title(request.title().trim())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();
    }

    public BreakItemResponse toResponse(EmployeeScheduleBreakEntity entity) {
        return new BreakItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime());
    }

    public ScheduleDayItemResponse toResponse(EmployeeScheduleDayEntity entity,
            List<EmployeeScheduleBreakEntity> breaks) {
        List<BreakItemResponse> breakResponses = breaks != null
                ? breaks.stream().map(this::toResponse).toList()
                : Collections.emptyList();

        return new ScheduleDayItemResponse(
                entity.getId(),
                entity.getDayOfWeek(),
                entity.isWorking(),
                entity.getStartTime(),
                entity.getEndTime(),
                breakResponses);
    }

    public EmployeeScheduleResponse toResponse(EmployeeScheduleEntity entity, List<ScheduleDayItemResponse> days) {
        return new EmployeeScheduleResponse(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getName(),
                entity.isDefault(),
                entity.isActive(),
                days != null ? days : Collections.emptyList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
