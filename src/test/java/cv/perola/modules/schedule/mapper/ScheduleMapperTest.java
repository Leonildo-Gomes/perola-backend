package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.BreakItemResponse;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleMapperTest {

    private final ScheduleMapper mapper = new ScheduleMapper();

    @Test
    @DisplayName("Deve mapear SaveEmployeeScheduleRequest para EmployeeScheduleEntity corretamente")
    void shouldMapSaveEmployeeScheduleRequestToEntity() {
        UUID employeeId = UUID.randomUUID();
        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "  Horário Padrão  ",
                true,
                true,
                Collections.emptyList());

        EmployeeScheduleEntity entity = mapper.toEntity(employeeId, request, true);

        assertThat(entity.getEmployeeId()).isEqualTo(employeeId);
        assertThat(entity.getName()).isEqualTo("Horário Padrão");
        assertThat(entity.isDefault()).isTrue();
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear ScheduleDayItemRequest trabalhado para EmployeeScheduleDayEntity com horas")
    void shouldMapWorkingScheduleDayItemRequestToEntity() {
        UUID scheduleId = UUID.randomUUID();
        ScheduleDayItemRequest request = new ScheduleDayItemRequest(
                DayOfWeek.MONDAY,
                true,
                LocalTime.of(8, 30),
                LocalTime.of(16, 0),
                Collections.emptyList());

        EmployeeScheduleDayEntity entity = mapper.toEntity(scheduleId, request);

        assertThat(entity.getEmployeeScheduleId()).isEqualTo(scheduleId);
        assertThat(entity.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(entity.isWorking()).isTrue();
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    @DisplayName("Deve mapear ScheduleDayItemRequest de folga para EmployeeScheduleDayEntity com horas nulas")
    void shouldMapNonWorkingScheduleDayItemRequestToEntityWithNullTimes() {
        UUID scheduleId = UUID.randomUUID();
        ScheduleDayItemRequest request = new ScheduleDayItemRequest(
                DayOfWeek.SUNDAY,
                false,
                null,
                null,
                Collections.emptyList());

        EmployeeScheduleDayEntity entity = mapper.toEntity(scheduleId, request);

        assertThat(entity.getEmployeeScheduleId()).isEqualTo(scheduleId);
        assertThat(entity.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(entity.isWorking()).isFalse();
        assertThat(entity.getStartTime()).isNull();
        assertThat(entity.getEndTime()).isNull();
    }

    @Test
    @DisplayName("Deve mapear BreakItemRequest para EmployeeScheduleBreakEntity com employeeScheduleDayId")
    void shouldMapBreakItemRequestToEntity() {
        UUID dayId = UUID.randomUUID();
        BreakItemRequest request = new BreakItemRequest("  Almoço  ", LocalTime.of(12, 30), LocalTime.of(13, 0));

        EmployeeScheduleBreakEntity entity = mapper.toEntity(dayId, request);

        assertThat(entity.getEmployeeScheduleDayId()).isEqualTo(dayId);
        assertThat(entity.getTitle()).isEqualTo("Almoço");
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(12, 30));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    @DisplayName("Deve mapear EmployeeScheduleDayEntity e pausas para ScheduleDayItemResponse")
    void shouldMapDayEntityAndBreaksToResponse() {
        UUID dayId = UUID.randomUUID();
        UUID breakId = UUID.randomUUID();

        EmployeeScheduleDayEntity dayEntity = EmployeeScheduleDayEntity.builder()
                .employeeScheduleId(UUID.randomUUID())
                .dayOfWeek(DayOfWeek.MONDAY)
                .isWorking(true)
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(16, 0))
                .build();
        dayEntity.setId(dayId);

        EmployeeScheduleBreakEntity breakEntity = EmployeeScheduleBreakEntity.builder()
                .id(breakId)
                .employeeScheduleDayId(dayId)
                .title("Almoço")
                .startTime(LocalTime.of(12, 30))
                .endTime(LocalTime.of(13, 0))
                .build();

        ScheduleDayItemResponse response = mapper.toResponse(dayEntity, List.of(breakEntity));

        assertThat(response.id()).isEqualTo(dayId);
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.isWorking()).isTrue();
        assertThat(response.startTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(response.breaks()).hasSize(1);
        BreakItemResponse b = response.breaks().getFirst();
        assertThat(b.id()).isEqualTo(breakId);
        assertThat(b.title()).isEqualTo("Almoço");
    }

    @Test
    @DisplayName("Deve mapear EmployeeScheduleEntity e dias para EmployeeScheduleResponse")
    void shouldMapScheduleEntityToResponse() {
        UUID scheduleId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant now = Instant.now();

        EmployeeScheduleEntity entity = EmployeeScheduleEntity.builder()
                .employeeId(employeeId)
                .name("Horário de Dezembro")
                .isDefault(false)
                .isActive(true)
                .build();
        entity.setId(scheduleId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        ScheduleDayItemResponse dayResp = new ScheduleDayItemResponse(
                UUID.randomUUID(),
                DayOfWeek.MONDAY,
                true,
                LocalTime.of(9, 30),
                LocalTime.of(18, 0),
                Collections.emptyList()
        );

        EmployeeScheduleResponse response = mapper.toResponse(entity, List.of(dayResp));

        assertThat(response.id()).isEqualTo(scheduleId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.name()).isEqualTo("Horário de Dezembro");
        assertThat(response.isDefault()).isFalse();
        assertThat(response.isActive()).isTrue();
        assertThat(response.days()).containsExactly(dayResp);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }
}
