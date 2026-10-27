package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.BreakItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleMapperTest {

    private ScheduleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ScheduleMapper();
    }

    @Test
    @DisplayName("Deve mapear WeeklyScheduleItemRequest para EmployeeScheduleEntity com valores padrão")
    void shouldMapWeeklyScheduleItemRequestToEntity() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        WeeklyScheduleItemRequest request = new WeeklyScheduleItemRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                null,
                null
        );

        // Act
        EmployeeScheduleEntity entity = mapper.toEntity(employeeId, request);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getEmployeeId()).isEqualTo(employeeId);
        assertThat(entity.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear BreakItemRequest para EmployeeScheduleBreakEntity")
    void shouldMapBreakItemRequestToEntity() {
        // Arrange
        UUID scheduleId = UUID.randomUUID();
        BreakItemRequest request = new BreakItemRequest("Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0));

        // Act
        EmployeeScheduleBreakEntity entity = mapper.toEntity(scheduleId, request);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getEmployeeScheduleId()).isEqualTo(scheduleId);
        assertThat(entity.getTitle()).isEqualTo("Almoço");
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    @DisplayName("Deve mapear EmployeeScheduleBreakEntity para BreakItemResponse")
    void shouldMapBreakEntityToResponse() {
        // Arrange
        UUID breakId = UUID.randomUUID();
        EmployeeScheduleBreakEntity entity = EmployeeScheduleBreakEntity.builder()
                .id(breakId)
                .employeeScheduleId(UUID.randomUUID())
                .title("Lanche")
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(16, 30))
                .build();

        // Act
        BreakItemResponse response = mapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(breakId);
        assertThat(response.title()).isEqualTo("Lanche");
        assertThat(response.startTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(16, 30));
    }

    @Test
    @DisplayName("Deve mapear EmployeeScheduleEntity e pausas para WeeklyScheduleItemResponse")
    void shouldMapScheduleEntityAndBreaksToItemResponse() {
        // Arrange
        UUID scheduleId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeScheduleEntity entity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .isActive(true)
                .build();
        entity.setId(scheduleId);

        EmployeeScheduleBreakEntity breakEntity = EmployeeScheduleBreakEntity.builder()
                .id(UUID.randomUUID())
                .employeeScheduleId(scheduleId)
                .title("Descanso")
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 0))
                .build();

        // Act
        WeeklyScheduleItemResponse response = mapper.toResponse(entity, List.of(breakEntity));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(scheduleId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.isActive()).isTrue();
        assertThat(response.breaks()).hasSize(1);
        assertThat(response.breaks().get(0).title()).isEqualTo("Descanso");
    }

    @Test
    @DisplayName("Deve mapear lista de itens para WeeklyScheduleResponse")
    void shouldMapItemsToWeeklyScheduleResponse() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        WeeklyScheduleItemResponse item = new WeeklyScheduleItemResponse(
                UUID.randomUUID(), employeeId, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(18, 0), true, List.of()
        );

        // Act
        WeeklyScheduleResponse response = mapper.toResponse(employeeId, List.of(item));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.schedules()).hasSize(1);
    }
}
