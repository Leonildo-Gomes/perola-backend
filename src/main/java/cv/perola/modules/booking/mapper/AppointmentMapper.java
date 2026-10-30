package cv.perola.modules.booking.mapper;

import cv.perola.core.config.AppConfig;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Mapper dedicado para conversão entre AppointmentEntity e DTOs do módulo Booking.
 */
@Component
public class AppointmentMapper {

    public AppointmentEntity toEntity(
            CreateAppointmentRequest request,
            UUID customerId,
            UUID createdById,
            ServiceResponse service,
            Instant startTime,
            Instant endTime
    ) {
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

    public AppointmentResponse toResponse(AppointmentEntity entity) {
        return toResponse(entity, ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE));
    }

    public AppointmentResponse toResponse(AppointmentEntity entity, ZoneId zoneId) {
        ZoneId targetZone = zoneId != null ? zoneId : ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
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
                toOffsetDateTime(entity.getCreatedAt(), targetZone)
        );
    }

    private OffsetDateTime toOffsetDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, zoneId);
    }
}

