package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.EmployeeMonthlyScheduleEntity;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyScheduleMapperTest {

    private final MonthlyScheduleMapper mapper = new MonthlyScheduleMapper();

    @Test
    @DisplayName("Deve mapear OpenMonthlyScheduleRequest para EmployeeMonthlyScheduleEntity")
    void shouldMapOpenMonthlyScheduleRequestToEntity() {
        UUID employeeId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Instant openedAt = Instant.parse("2026-10-29T18:00:00Z");

        OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                scheduleId,
                2026,
                11,
                Collections.emptyList()
        );

        EmployeeMonthlyScheduleEntity entity = mapper.toEntity(employeeId, request, openedAt);

        assertThat(entity.getEmployeeId()).isEqualTo(employeeId);
        assertThat(entity.getEmployeeScheduleId()).isEqualTo(scheduleId);
        assertThat(entity.getYear()).isEqualTo(2026);
        assertThat(entity.getMonth()).isEqualTo(11);
        assertThat(entity.isOpened()).isTrue();
        assertThat(entity.getOpenedAt()).isEqualTo(openedAt);
    }

    @Test
    @DisplayName("Deve mapear EmployeeMonthlyScheduleEntity para MonthlyScheduleResponse com nome do horário")
    void shouldMapEntityToMonthlyScheduleResponse() {
        UUID id = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Instant openedAt = Instant.parse("2026-10-29T18:00:00Z");

        EmployeeMonthlyScheduleEntity entity = EmployeeMonthlyScheduleEntity.builder()
                .employeeId(employeeId)
                .employeeScheduleId(scheduleId)
                .year(2026)
                .month(12)
                .isOpened(true)
                .openedAt(openedAt)
                .build();
        entity.setId(id);

        MonthlyScheduleResponse response = mapper.toResponse(entity, "Horário de Dezembro");

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.scheduleId()).isEqualTo(scheduleId);
        assertThat(response.scheduleName()).isEqualTo("Horário de Dezembro");
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(12);
        assertThat(response.isOpened()).isTrue();
        assertThat(response.openedAt()).isEqualTo(openedAt);
    }
}

