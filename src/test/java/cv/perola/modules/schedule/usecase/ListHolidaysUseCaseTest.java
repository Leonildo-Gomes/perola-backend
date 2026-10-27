package cv.perola.modules.schedule.usecase;

import cv.perola.modules.schedule.domain.HolidayEntity;
import cv.perola.modules.schedule.dto.HolidayResponse;
import cv.perola.modules.schedule.mapper.HolidayMapper;
import cv.perola.modules.schedule.repository.HolidayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListHolidaysUseCaseTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private HolidayMapper holidayMapper;

    @InjectMocks
    private ListHolidaysUseCase listHolidaysUseCase;

    @Test
    @DisplayName("Deve listar todos os feriados quando ano não for informado")
    void shouldListAllHolidaysWhenYearNotProvided() {
        // Arrange
        HolidayEntity entity = HolidayEntity.builder()
                .id(UUID.randomUUID())
                .holidayDate(LocalDate.of(2026, 1, 1))
                .name("Ano Novo")
                .isNational(true)
                .build();
        HolidayResponse response = new HolidayResponse(entity.getId(), entity.getHolidayDate(), entity.getName(), true);

        when(holidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(List.of(entity));
        when(holidayMapper.toResponse(entity)).thenReturn(response);

        // Act
        List<HolidayResponse> result = listHolidaysUseCase.execute(null);

        // Assert
        assertThat(result).hasSize(1).contains(response);
        verify(holidayRepository).findAllByOrderByHolidayDateAsc();
    }

    @Test
    @DisplayName("Deve filtrar feriados por ano quando informado")
    void shouldFilterHolidaysByYearWhenProvided() {
        // Arrange
        int year = 2026;
        HolidayEntity entity = HolidayEntity.builder()
                .id(UUID.randomUUID())
                .holidayDate(LocalDate.of(2026, 5, 1))
                .name("Dia do Trabalhador")
                .isNational(true)
                .build();
        HolidayResponse response = new HolidayResponse(entity.getId(), entity.getHolidayDate(), entity.getName(), true);

        when(holidayRepository.findAllByYear(year)).thenReturn(List.of(entity));
        when(holidayMapper.toResponse(entity)).thenReturn(response);

        // Act
        List<HolidayResponse> result = listHolidaysUseCase.execute(year);

        // Assert
        assertThat(result).hasSize(1).contains(response);
        verify(holidayRepository).findAllByYear(year);
    }
}
