package cv.perola.modules.booking.mapper;

import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.ReviewEntity;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMapperTest {

    private final ReviewMapper reviewMapper = new ReviewMapper();

    @Test
    @DisplayName("Deve converter AppointmentEntity e CreateReviewRequest em ReviewEntity")
    void shouldConvertAppointmentAndRequestToReviewEntity() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .customerId(customerId)
                .employeeId(employeeId)
                .build();
        ReflectionTestUtils.setField(appointment, "id", appointmentId);
        CreateReviewRequest request = new CreateReviewRequest(5, "Ótimo atendimento!");

        // Act
        ReviewEntity entity = reviewMapper.toEntity(appointment, request);

        // Assert
        assertThat(entity.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(entity.getCustomerId()).isEqualTo(customerId);
        assertThat(entity.getEmployeeId()).isEqualTo(employeeId);
        assertThat(entity.getRating()).isEqualTo(5);
        assertThat(entity.getComment()).isEqualTo("Ótimo atendimento!");
    }

    @Test
    @DisplayName("Deve converter ReviewEntity em ReviewResponse com fuso horário do salão")
    void shouldConvertReviewEntityToReviewResponseWithBusinessZone() {
        // Arrange
        UUID reviewId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-03T12:30:00Z");
        ReviewEntity entity = ReviewEntity.builder()
                .appointmentId(appointmentId)
                .customerId(customerId)
                .employeeId(employeeId)
                .rating(4)
                .comment("Muito bom")
                .build();
        ReflectionTestUtils.setField(entity, "id", reviewId);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ZoneId zoneId = ZoneId.of("Atlantic/Cape_Verde");

        // Act
        ReviewResponse response = reviewMapper.toResponse(entity, zoneId);

        // Assert
        assertThat(response.id()).isEqualTo(reviewId);
        assertThat(response.appointmentId()).isEqualTo(appointmentId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("Muito bom");
        assertThat(response.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(-1));
        assertThat(response.createdAt().getHour()).isEqualTo(11);
    }
}

