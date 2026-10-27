package cv.perola.modules.schedule.mapper;

import cv.perola.modules.schedule.domain.HolidayEntity;
import cv.perola.modules.schedule.dto.HolidayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayMapperTest {

    private HolidayMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new HolidayMapper();
    }

    @Test
    @DisplayName("Deve mapear HolidayEntity para HolidayResponse")
    void shouldMapHolidayEntityToResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        HolidayEntity entity = HolidayEntity.builder()
                .id(id)
                .holidayDate(LocalDate.of(2026, 7, 5))
                .name("Dia da Independência")
                .isNational(true)
                .build();

        // Act
        HolidayResponse response = mapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.holidayDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(response.name()).isEqualTo("Dia da Independência");
        assertThat(response.isNational()).isTrue();
    }
}
