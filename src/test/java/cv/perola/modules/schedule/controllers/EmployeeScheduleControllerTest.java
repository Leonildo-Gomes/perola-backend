package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.usecase.CreateEmployeeScheduleUseCase;
import cv.perola.modules.schedule.usecase.GetEmployeeScheduleUseCase;
import cv.perola.modules.schedule.usecase.ListEmployeeSchedulesUseCase;
import cv.perola.modules.schedule.usecase.UpdateEmployeeScheduleUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeScheduleControllerTest {

    @Mock
    private CreateEmployeeScheduleUseCase createEmployeeScheduleUseCase;

    @Mock
    private ListEmployeeSchedulesUseCase listEmployeeSchedulesUseCase;

    @Mock
    private GetEmployeeScheduleUseCase getEmployeeScheduleUseCase;

    @Mock
    private UpdateEmployeeScheduleUseCase updateEmployeeScheduleUseCase;

    @InjectMocks
    private EmployeeScheduleController controller;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(), "owner@perola.cv", "hash",
            List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
    );

    @Test
    @DisplayName("Deve criar perfil de horário e retornar HTTP 201 Created")
    void shouldCreateScheduleAndReturn201() {
        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário Padrão", true, true, Collections.emptyList()
        );
        EmployeeScheduleResponse expected = new EmployeeScheduleResponse(
                scheduleId, employeeId, "Horário Padrão", true, true, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(createEmployeeScheduleUseCase.execute(employeeId, request, principal)).thenReturn(expected);

        ResponseEntity<EmployeeScheduleResponse> response = controller.createSchedule(employeeId, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(createEmployeeScheduleUseCase).execute(employeeId, request, principal);
    }

    @Test
    @DisplayName("Deve listar perfis de horário do profissional e retornar HTTP 200 OK")
    void shouldListSchedulesAndReturn200() {
        EmployeeScheduleResponse item = new EmployeeScheduleResponse(
                scheduleId, employeeId, "Horário Padrão", true, true, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(listEmployeeSchedulesUseCase.execute(employeeId, principal)).thenReturn(List.of(item));

        ResponseEntity<List<EmployeeScheduleResponse>> response = controller.listSchedules(employeeId, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(item);
        verify(listEmployeeSchedulesUseCase).execute(employeeId, principal);
    }

    @Test
    @DisplayName("Deve obter perfil de horário específico e retornar HTTP 200 OK")
    void shouldGetScheduleByIdAndReturn200() {
        EmployeeScheduleResponse expected = new EmployeeScheduleResponse(
                scheduleId, employeeId, "Horário Padrão", true, true, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(getEmployeeScheduleUseCase.execute(employeeId, scheduleId, principal)).thenReturn(expected);

        ResponseEntity<EmployeeScheduleResponse> response = controller.getSchedule(employeeId, scheduleId, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(getEmployeeScheduleUseCase).execute(employeeId, scheduleId, principal);
    }

    @Test
    @DisplayName("Deve atualizar perfil de horário existente e retornar HTTP 200 OK")
    void shouldUpdateScheduleAndReturn200() {
        SaveEmployeeScheduleRequest request = new SaveEmployeeScheduleRequest(
                "Horário de Dezembro", false, true, Collections.emptyList()
        );
        EmployeeScheduleResponse expected = new EmployeeScheduleResponse(
                scheduleId, employeeId, "Horário de Dezembro", false, true, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(updateEmployeeScheduleUseCase.execute(employeeId, scheduleId, request, principal)).thenReturn(expected);

        ResponseEntity<EmployeeScheduleResponse> response = controller.updateSchedule(employeeId, scheduleId, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(updateEmployeeScheduleUseCase).execute(employeeId, scheduleId, request, principal);
    }
}
