package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeTimeOffEntity;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimeOffMapperTest {

    private TimeOffMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TimeOffMapper();
    }

    @Test
    @DisplayName("Deve mapear CreateTimeOffRequest de dia inteiro para EmployeeTimeOffEntity")
    void shouldMapAllDayRequestToEntity() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 15),
                true,
                null,
                null,
                TimeOffType.VACATION,
                "Férias anuais"
        );

        // Act
        EmployeeTimeOffEntity entity = mapper.toEntity(employeeId, request);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getEmployeeId()).isEqualTo(employeeId);
        assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(entity.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(entity.isAllDay()).isTrue();
        assertThat(entity.getStartTime()).isNull();
        assertThat(entity.getEndTime()).isNull();
        assertThat(entity.getType()).isEqualTo(TimeOffType.VACATION);
        assertThat(entity.getNote()).isEqualTo("Férias anuais");
    }

    @Test
    @DisplayName("Deve mapear CreateTimeOffRequest parcial para EmployeeTimeOffEntity")
    void shouldMapPartialDayRequestToEntity() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 10),
                false,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                TimeOffType.SICK_LEAVE,
                "Consulta médica"
        );

        // Act
        EmployeeTimeOffEntity entity = mapper.toEntity(employeeId, request);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.isAllDay()).isFalse();
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(entity.getType()).isEqualTo(TimeOffType.SICK_LEAVE);
    }

    @Test
    @DisplayName("Deve mapear EmployeeTimeOffEntity para TimeOffResponse")
    void shouldMapEntityToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeTimeOffEntity entity = EmployeeTimeOffEntity.builder()
                .employeeId(employeeId)
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 15))
                .isAllDay(true)
                .type(TimeOffType.VACATION)
                .note("Descanso")
                .build();
        entity.setId(id);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        // Act
        TimeOffResponse response = mapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(response.isAllDay()).isTrue();
        assertThat(response.type()).isEqualTo(TimeOffType.VACATION);
        assertThat(response.note()).isEqualTo("Descanso");
    }
}
