package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.SetWeeklyScheduleRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleItemResponse;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import cv.perola.modules.schedule.usecase.GetWeeklyScheduleUseCase;
import cv.perola.modules.schedule.usecase.SetWeeklyScheduleUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleWeeklyControllerTest {

    @Mock
    private SetWeeklyScheduleUseCase setWeeklyScheduleUseCase;

    @Mock
    private GetWeeklyScheduleUseCase getWeeklyScheduleUseCase;

    @InjectMocks
    private ScheduleWeeklyController controller;

    @Test
    @DisplayName("Deve delegar setWeeklySchedule para use case e retornar 200 OK")
    void shouldDelegateSetWeeklyScheduleAndReturn200() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "pwd", List.of("OWNER"), List.of(), true);
        SetWeeklyScheduleRequest request = new SetWeeklyScheduleRequest(List.of(
                new WeeklyScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, null)
        ));
        WeeklyScheduleResponse expectedResponse = new WeeklyScheduleResponse(employeeId, List.of(
                new WeeklyScheduleItemResponse(UUID.randomUUID(), employeeId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0), true, List.of())
        ));
        when(setWeeklyScheduleUseCase.execute(employeeId, request, principal)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<WeeklyScheduleResponse> response = controller.setWeeklySchedule(employeeId, request, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(setWeeklyScheduleUseCase).execute(employeeId, request, principal);
    }

    @Test
    @DisplayName("Deve delegar getWeeklySchedule para use case e retornar 200 OK")
    void shouldDelegateGetWeeklyScheduleAndReturn200() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "pwd", List.of("OWNER"), List.of(), true);
        WeeklyScheduleResponse expectedResponse = new WeeklyScheduleResponse(employeeId, List.of());
        when(getWeeklyScheduleUseCase.execute(employeeId, principal)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<WeeklyScheduleResponse> response = controller.getWeeklySchedule(employeeId, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(getWeeklyScheduleUseCase).execute(employeeId, principal);
    }
}
