package cv.perola.modules.schedule.controllers;

import cv.perola.modules.schedule.dto.AvailabilitySlotsResponse;
import cv.perola.modules.schedule.dto.AvailableSlotResponse;
import cv.perola.modules.schedule.usecase.CalculateAvailableSlotsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private CalculateAvailableSlotsUseCase calculateAvailableSlotsUseCase;

    @InjectMocks
    private AvailabilityController controller;

    @Test
    @DisplayName("Deve retornar HTTP 200 OK com os slots disponíveis calculados pelo use case")
    void shouldReturn200OkWithCalculatedAvailableSlots() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 3);
        AvailabilitySlotsResponse expected = new AvailabilitySlotsResponse(
                date,
                employeeId,
                serviceId,
                60,
                List.of(
                        new AvailableSlotResponse(
                                OffsetDateTime.of(2026, 11, 3, 8, 30, 0, 0, ZoneOffset.ofHours(-1)),
                                OffsetDateTime.of(2026, 11, 3, 9, 30, 0, 0, ZoneOffset.ofHours(-1))
                        )
                )
        );
        when(calculateAvailableSlotsUseCase.execute(businessId, employeeId, serviceId, date))
                .thenReturn(expected);

        // Act
        ResponseEntity<AvailabilitySlotsResponse> response = controller.getAvailableSlots(
                businessId, employeeId, serviceId, date
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(calculateAvailableSlotsUseCase).execute(businessId, employeeId, serviceId, date);
    }
}

