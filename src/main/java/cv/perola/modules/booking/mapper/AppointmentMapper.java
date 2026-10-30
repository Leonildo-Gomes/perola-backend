package cv.perola.modules.booking.mapper;

import cv.perola.core.config.AppConfig;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.AppointmentHistoryEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentHistoryResponse;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Mapper dedicado para conversão entre AppointmentEntity,
 * AppointmentHistoryEntity e DTOs do módulo Booking.
 */
@Component
public class AppointmentMapper {

    public AppointmentEntity toEntity(
            CreateAppointmentRequest request,
            UUID customerId,
            UUID createdById,
            ServiceResponse service,
            Instant startTime,
            Instant endTime) {
        return AppointmentEntity.builder()
                .businessId(request.businessId())
                .customerId(customerId)
                .employeeId(request.employeeId())
                .serviceId(request.serviceId())
                .createdById(createdById)
                .startTime(startTime)
                .endTime(endTime)
                .serviceName(service.name())
                .servicePrice(service.price())
                .serviceDuration(service.durationMinutes())
                .status(AppointmentStatus.PENDING)
                .notes(request.notes())
                .build();
    }

    public AppointmentHistoryEntity toHistoryEntity(
            UUID appointmentId,
            UUID performedById,
            AppointmentAction action,
            AppointmentStatus fromStatus,
            AppointmentStatus toStatus,
            Instant oldStartTime,
            Instant oldEndTime,
            Instant newStartTime,
            Instant newEndTime,
            String notes) {
        return AppointmentHistoryEntity.builder()
                .appointmentId(appointmentId)
                .performedById(performedById)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .oldStartTime(oldStartTime)
                .oldEndTime(oldEndTime)
                .newStartTime(newStartTime)
                .newEndTime(newEndTime)
                .notes(notes)
                .build();
    }

    public AppointmentHistoryResponse toHistoryResponse(AppointmentHistoryEntity entity, ZoneId zoneId) {
        ZoneId targetZone = zoneId != null ? zoneId : ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        return new AppointmentHistoryResponse(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getPerformedById(),
                entity.getAction(),
                entity.getFromStatus(),
                entity.getToStatus(),
                toOffsetDateTime(entity.getOldStartTime(), targetZone),
                toOffsetDateTime(entity.getOldEndTime(), targetZone),
                toOffsetDateTime(entity.getNewStartTime(), targetZone),
                toOffsetDateTime(entity.getNewEndTime(), targetZone),
                entity.getNotes(),
                toOffsetDateTime(entity.getCreatedAt(), targetZone));
    }

    public AppointmentResponse toResponse(AppointmentEntity entity) {
        return toResponse(entity, List.of(), ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE));
    }

    public AppointmentResponse toResponse(AppointmentEntity entity, ZoneId zoneId) {
        return toResponse(entity, List.of(), zoneId);
    }

    public AppointmentResponse toResponse(
            AppointmentEntity entity,
            List<AppointmentHistoryEntity> historyEntities,
            ZoneId zoneId) {
        ZoneId targetZone = zoneId != null ? zoneId : ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        List<AppointmentHistoryResponse> history = historyEntities == null
                ? List.of()
                : historyEntities.stream()
                        .map(h -> toHistoryResponse(h, targetZone))
                        .toList();

        return new AppointmentResponse(
                entity.getId(),
                entity.getBusinessId(),
                entity.getCustomerId(),
                entity.getEmployeeId(),
                entity.getServiceId(),
                entity.getServiceName(),
                entity.getServicePrice(),
                entity.getServiceDuration(),
                toOffsetDateTime(entity.getStartTime(), targetZone),
                toOffsetDateTime(entity.getEndTime(), targetZone),
                toOffsetDateTime(entity.getProposedStartTime(), targetZone),
                toOffsetDateTime(entity.getProposedEndTime(), targetZone),
                entity.getStatus(),
                entity.getNotes(),
                toOffsetDateTime(entity.getCreatedAt(), targetZone),
                history);
    }

    private OffsetDateTime toOffsetDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, zoneId);
    }
}
