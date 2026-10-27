package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import cv.perola.modules.schedule.usecase.CreateTimeOffUseCase;
import cv.perola.modules.schedule.usecase.DeleteTimeOffUseCase;
import cv.perola.modules.schedule.usecase.ListTimeOffUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleTimeOffControllerTest {

    @Mock
    private CreateTimeOffUseCase createTimeOffUseCase;

    @Mock
    private ListTimeOffUseCase listTimeOffUseCase;

    @Mock
    private DeleteTimeOffUseCase deleteTimeOffUseCase;

    @InjectMocks
    private ScheduleTimeOffController controller;

    @Test
    @DisplayName("Deve delegar createTimeOff para use case e retornar 201 Created")
    void shouldDelegateCreateTimeOffAndReturn201() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "pwd", List.of("OWNER"), List.of(), true);
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), true, null, null, TimeOffType.VACATION, "Férias"
        );
        TimeOffResponse expectedResponse = new TimeOffResponse(
                UUID.randomUUID(), employeeId, request.startDate(), request.endDate(), true,
                null, null, TimeOffType.VACATION, "Férias", Instant.now(), Instant.now()
        );
        when(createTimeOffUseCase.execute(employeeId, request, principal)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<TimeOffResponse> response = controller.createTimeOff(employeeId, request, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(createTimeOffUseCase).execute(employeeId, request, principal);
    }

    @Test
    @DisplayName("Deve delegar listTimeOff para use case e retornar 200 OK")
    void shouldDelegateListTimeOffAndReturn200() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "pwd", List.of("OWNER"), List.of(), true);
        when(listTimeOffUseCase.execute(employeeId, principal)).thenReturn(List.of());

        // Act
        ResponseEntity<List<TimeOffResponse>> response = controller.listTimeOff(employeeId, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
        verify(listTimeOffUseCase).execute(employeeId, principal);
    }

    @Test
    @DisplayName("Deve delegar deleteTimeOff para use case e retornar 204 No Content")
    void shouldDelegateDeleteTimeOffAndReturn204() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UUID timeOffId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner@perola.cv", "pwd", List.of("OWNER"), List.of(), true);

        // Act
        ResponseEntity<Void> response = controller.deleteTimeOff(employeeId, timeOffId, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(deleteTimeOffUseCase).execute(employeeId, timeOffId, principal);
    }
}
