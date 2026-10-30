package cv.perola.modules.notifications.mapper;

import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.modules.notifications.mapper.NotificationMapper.BookingNotificationPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    @Test
    @DisplayName("Deve converter BookingDomainEvent em BookingNotificationPayload com mensagem de resumo")
    void shouldMapDomainEventToNotificationPayload() {
        // Arrange
        NotificationMapper mapper = new NotificationMapper();
        UUID appointmentId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Instant now = Instant.now();

        AppointmentLifecycleEvent event = new AppointmentLifecycleEvent(
                appointmentId,
                businessId,
                customerId,
                employeeId,
                customerId,
                "CREATED",
                null,
                "PENDING",
                now,
                now.plusSeconds(3600),
                "Nota",
                now
        );

        // Act
        BookingNotificationPayload payload = mapper.toResponse(event);

        // Assert
        assertThat(payload.appointmentId()).isEqualTo(appointmentId);
        assertThat(payload.businessId()).isEqualTo(businessId);
        assertThat(payload.customerId()).isEqualTo(customerId);
        assertThat(payload.employeeId()).isEqualTo(employeeId);
        assertThat(payload.action()).isEqualTo("CREATED");
        assertThat(payload.toStatus()).isEqualTo("PENDING");
        assertThat(payload.summaryMessage()).contains(appointmentId.toString(), "PENDING", "CREATED");
    }
}

