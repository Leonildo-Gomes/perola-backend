package cv.perola.modules.booking.mapper;

import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class ReviewMapper {

    public ReviewEntity toEntity(AppointmentEntity appointment, CreateReviewRequest request) {
        return ReviewEntity.builder()
                .appointmentId(appointment.getId())
                .customerId(appointment.getCustomerId())
                .employeeId(appointment.getEmployeeId())
                .rating(request.rating())
                .comment(request.comment())
                .build();
    }

    public ReviewResponse toResponse(ReviewEntity entity, ZoneId zoneId) {
        Instant createdInstant = entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now();
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(createdInstant, zoneId);
        return new ReviewResponse(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getCustomerId(),
                entity.getEmployeeId(),
                entity.getRating(),
                entity.getComment(),
                createdAt
        );
    }
}

