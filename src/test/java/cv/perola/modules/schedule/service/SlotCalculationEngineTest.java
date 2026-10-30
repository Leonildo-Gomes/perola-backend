package cv.perola.modules.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCalculationEngineTest {

    private static final ZoneId CAPE_VERDE_ZONE = ZoneId.of("Atlantic/Cape_Verde");
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 2); // Monday
    private static final Instant FUTURE_REFERENCE_NOW = ZonedDateTime.of(
            LocalDate.of(2026, 10, 1), LocalTime.of(8, 0), CAPE_VERDE_ZONE
    ).toInstant();

    private SlotCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SlotCalculationEngine();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o mês não está aberto (monthOpened = false)")
    void shouldReturnEmptyListWhenMonthIsNotOpened() {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                false,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                List.of(),
                false,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o dia não é de trabalho (workingDay = false)")
    void shouldReturnEmptyListWhenDayIsNotWorking() {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                false,
                null,
                null,
                List.of(),
                false,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando há ausência ou feriado de dia inteiro (allDayTimeOff = true)")
    void shouldReturnEmptyListWhenAllDayTimeOffIsPresent() {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                List.of(),
                true,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("Deve fatiar corretamente um expediente contínuo sem pausas")
    void shouldSliceContinuousWorkingShiftCorrectly() {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                List.of(),
                false,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).containsExactly(
                interval(8, 0, 9, 0),
                interval(9, 0, 10, 0),
                interval(10, 0, 11, 0),
                interval(11, 0, 12, 0)
        );
    }

    @Test
    @DisplayName("Deve subtrair pausa de almoço de 30 minutos (08:30–16:00 com pausa 12:30–13:00) alinhando a tarde às 13:00")
    void shouldSubtractThirtyMinuteLunchBreakAndAlignAfternoonAt1300() {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                true,
                LocalTime.of(8, 30),
                LocalTime.of(16, 0),
                List.of(new LocalTimeWindow(LocalTime.of(12, 30), LocalTime.of(13, 0))),
                false,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).containsExactly(
                interval(8, 30, 9, 30),
                interval(9, 30, 10, 30),
                interval(10, 30, 11, 30),
                interval(11, 30, 12, 30),
                interval(13, 0, 14, 0),
                interval(14, 0, 15, 0),
                interval(15, 0, 16, 0)
        );
    }

    @Test
    @DisplayName("Deve subtrair múltiplas pausas no mesmo dia (café da manhã e almoço)")
    void shouldSubtractMultipleBreaksInSameWorkingShift() {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(16, 30),
                List.of(
                        new LocalTimeWindow(LocalTime.of(10, 0), LocalTime.of(10, 30)),
                        new LocalTimeWindow(LocalTime.of(12, 30), LocalTime.of(13, 30))
                ),
                false,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).containsExactly(
                interval(8, 0, 9, 0),
                interval(9, 0, 10, 0),
                interval(10, 30, 11, 30),
                interval(11, 30, 12, 30),
                interval(13, 30, 14, 30),
                interval(14, 30, 15, 30),
                interval(15, 30, 16, 30)
        );
    }

    @Test
    @DisplayName("Deve subtrair ausências parciais e agendamentos encadeados preservando slots adjacentes")
    void shouldSubtractPartialTimeOffAndChainedAppointments() {
        // Arrange
        List<LocalTimeWindow> breaks = List.of(
                new LocalTimeWindow(LocalTime.of(12, 0), LocalTime.of(13, 0))
        );
        List<LocalTimeWindow> partialTimeOffs = List.of(
                new LocalTimeWindow(LocalTime.of(14, 0), LocalTime.of(15, 0))
        );
        List<TimeInterval> bookedIntervals = List.of(
                interval(9, 0, 10, 0),
                interval(10, 0, 11, 0)
        );

        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                breaks,
                false,
                partialTimeOffs,
                bookedIntervals,
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).containsExactly(
                interval(8, 0, 9, 0),
                interval(11, 0, 12, 0),
                interval(13, 0, 14, 0),
                interval(15, 0, 16, 0)
        );
    }

    @Test
    @DisplayName("Deve eliminar slots no passado mantendo o alinhamento da grelha de horários do dia")
    void shouldFilterOutPastSlotsWhilePreservingShiftGridAlignment() {
        // Arrange
        Instant currentDayAt0915 = ZonedDateTime.of(TARGET_DATE, LocalTime.of(9, 15), CAPE_VERDE_ZONE).toInstant();
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                60,
                true,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                List.of(),
                false,
                List.of(),
                List.of(),
                currentDayAt0915
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).containsExactly(
                interval(10, 0, 11, 0),
                interval(11, 0, 12, 0)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "30, 8",
            "45, 5",
            "60, 4",
            "90, 2",
            "120, 2"
    })
    @DisplayName("Deve calcular a quantidade correta de slots numa janela de 4 horas para diferentes durações de serviço")
    void shouldCalculateExpectedNumberOfSlotsForVariousDurations(int durationMinutes, int expectedCount) {
        // Arrange
        SlotCalculationInput input = new SlotCalculationInput(
                TARGET_DATE,
                CAPE_VERDE_ZONE,
                durationMinutes,
                true,
                true,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                List.of(),
                false,
                List.of(),
                List.of(),
                FUTURE_REFERENCE_NOW
        );

        // Act
        List<TimeInterval> slots = engine.calculateSlots(input);

        // Assert
        assertThat(slots).hasSize(expectedCount);
    }

    private TimeInterval interval(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeInterval(
                ZonedDateTime.of(TARGET_DATE, LocalTime.of(startHour, startMinute), CAPE_VERDE_ZONE).toInstant(),
                ZonedDateTime.of(TARGET_DATE, LocalTime.of(endHour, endMinute), CAPE_VERDE_ZONE).toInstant()
        );
    }
}

