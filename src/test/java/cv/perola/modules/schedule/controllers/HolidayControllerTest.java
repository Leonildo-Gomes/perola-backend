package cv.perola.modules.schedule.controllers;

import cv.perola.modules.schedule.dto.HolidayResponse;
import cv.perola.modules.schedule.usecase.ListHolidaysUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayControllerTest {

    @Mock
    private ListHolidaysUseCase listHolidaysUseCase;

    @InjectMocks
    private HolidayController controller;

    @Test
    @DisplayName("Deve delegar listHolidays para use case e retornar 200 OK")
    void shouldDelegateListHolidaysAndReturn200() {
        // Arrange
        Integer year = 2026;
        HolidayResponse holiday = new HolidayResponse(UUID.randomUUID(), LocalDate.of(2026, 1, 1), "Ano Novo", true);
        when(listHolidaysUseCase.execute(year)).thenReturn(List.of(holiday));

        // Act
        ResponseEntity<List<HolidayResponse>> response = controller.listHolidays(year);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(holiday);
        verify(listHolidaysUseCase).execute(year);
    }
}
