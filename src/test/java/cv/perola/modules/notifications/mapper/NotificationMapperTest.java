package cv.perola.modules.notifications.mapper;

import cv.perola.core.config.MailProperties;
import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.core.shared.event.BookingEventAction;
import cv.perola.modules.notifications.domain.enums.EmailTemplate;
import cv.perola.modules.notifications.dto.EmailMessageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final MailProperties mailProperties = new MailProperties(
            false,
            "no-reply@perola.cv",
            "Pérola Cabo Verde",
            "https://perola.cv"
    );
    private final NotificationMapper mapper = new NotificationMapper(mailProperties);

    @ParameterizedTest
    @CsvSource({
            "CREATED, APPOINTMENT_CREATED",
            "CONFIRMED, APPOINTMENT_CONFIRMED",
            "RESCHEDULE_CONFIRMED, APPOINTMENT_CONFIRMED",
            "CANCELLED, APPOINTMENT_CANCELLED",
            "RESCHEDULE_REQUESTED, APPOINTMENT_RESCHEDULED"
    })
    @DisplayName("Deve mapear BookingEventAction elegível para o EmailTemplate correspondente no fuso de Cabo Verde")
    void shouldMapActionToExpectedEmailTemplate(BookingEventAction action, String expectedCase) {
        // Arrange
        Instant startUtc = Instant.parse("2026-11-25T11:00:00Z"); // 10:00 em Atlantic/Cape_Verde (UTC-1)
        Instant endUtc = Instant.parse("2026-11-25T12:00:00Z");   // 11:00 em Atlantic/Cape_Verde (UTC-1)

        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(UUID.randomUUID())
                .businessId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .action(action)
                .toStatus("PENDING")
                .startTime(startUtc)
                .endTime(endUtc)
                .notes("Primeira marcação")
                .occurredAt(startUtc)
                .customerEmail("cliente@perola.cv")
                .customerName("Ana Silva")
                .businessName("Salão Pérola")
                .employeeName("Carla")
                .serviceName("Corte & Brushing")
                .servicePriceCve("2500.00")
                .businessTimeZone("Atlantic/Cape_Verde")
                .build();

        EmailTemplate expectedTemplate = "APPOINTMENT_RESCHEDULED".equals(expectedCase)
                ? EmailTemplate.RESCHEDULE_REQUESTED
                : EmailTemplate.valueOf(expectedCase);

        // Act
        Optional<EmailMessageRequest> result = mapper.toEmailRequest(event);

        // Assert
        assertThat(result).isPresent();
        EmailMessageRequest request = result.orElseThrow();
        assertThat(request.to()).isEqualTo("cliente@perola.cv");
        assertThat(request.recipientName()).isEqualTo("Ana Silva");
        assertThat(request.template()).isEqualTo(expectedTemplate);
        assertThat(request.subject()).contains("Salão Pérola");
        assertThat(request.variables())
                .containsEntry("appointmentDate", "25/11/2026")
                .containsEntry("startTime", "10:00")
                .containsEntry("endTime", "11:00")
                .containsEntry("myAppointmentsUrl", "https://perola.cv/appointments");
    }

    @ParameterizedTest
    @EnumSource(value = BookingEventAction.class, names = {"COMPLETED", "RESCHEDULE_REJECTED", "CANCEL_REQUESTED"})
    @DisplayName("Deve retornar Optional vazio para ações que não disparam template de e-mail")
    void shouldReturnEmptyOptionalForNonEmailableActions(BookingEventAction action) {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(UUID.randomUUID())
                .action(action)
                .startTime(now)
                .endTime(now.plusSeconds(1800))
                .customerEmail("cliente@perola.cv")
                .build();

        // Act
        Optional<EmailMessageRequest> result = mapper.toEmailRequest(event);

        // Assert
        assertThat(result).isEmpty();
    }
}
