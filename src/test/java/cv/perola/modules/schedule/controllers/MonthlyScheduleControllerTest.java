package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import cv.perola.modules.schedule.usecase.monthly.GetMonthlyScheduleUseCase;
import cv.perola.modules.schedule.usecase.monthly.OpenMonthlyScheduleUseCase;
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
class MonthlyScheduleControllerTest {

    @Mock
    private OpenMonthlyScheduleUseCase openMonthlyScheduleUseCase;

    @Mock
    private GetMonthlyScheduleUseCase getMonthlyScheduleUseCase;

    @InjectMocks
    private MonthlyScheduleController controller;

    private final UUID businessId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(), "owner@perola.cv", "hash",
            List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
    );

    @Test
    @DisplayName("Deve abrir agenda mensal e retornar HTTP 200 OK")
    void shouldOpenMonthlyScheduleAndReturn200() {
        OpenMonthlyScheduleRequest request = new OpenMonthlyScheduleRequest(
                scheduleId, 2026, 11, Collections.emptyList()
        );
        MonthlyScheduleResponse expected = new MonthlyScheduleResponse(
                UUID.randomUUID(), employeeId, scheduleId, "Horário Padrão", 2026, 11, true, Instant.now()
        );

        when(openMonthlyScheduleUseCase.execute(employeeId, request, principal)).thenReturn(expected);

        ResponseEntity<MonthlyScheduleResponse> response = controller.openMonthlySchedule(employeeId, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(openMonthlyScheduleUseCase).execute(employeeId, request, principal);
    }

    @Test
    @DisplayName("Deve consultar status de abertura mensal e retornar HTTP 200 OK")
    void shouldGetMonthlyScheduleAndReturn200() {
        MonthlyScheduleResponse expected = new MonthlyScheduleResponse(
                UUID.randomUUID(), employeeId, scheduleId, "Horário Padrão", 2026, 11, true, Instant.now()
        );

        when(getMonthlyScheduleUseCase.execute(employeeId, 2026, 11, principal)).thenReturn(expected);

        ResponseEntity<MonthlyScheduleResponse> response = controller.getMonthlySchedule(employeeId, 2026, 11, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(getMonthlyScheduleUseCase).execute(employeeId, 2026, 11, principal);
    }
}
