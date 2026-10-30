package cv.perola.modules.notifications.usecase.notification;

import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.modules.notifications.mapper.NotificationMapper;
import cv.perola.modules.notifications.mapper.NotificationMapper.BookingNotificationPayload;
import cv.perola.modules.notifications.service.NotificationService;
import cv.perola.modules.notifications.validator.notification.SendNotificationValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendBookingNotificationUseCaseTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationService notificationService;

    @Spy
    private SendNotificationValidator notificationValidator = new SendNotificationValidator();

    @InjectMocks
    private SendBookingNotificationUseCase sendBookingNotificationUseCase;

    @Test
    @DisplayName("Deve mapear o evento de domínio e despachar a notificação")
    void shouldMapEventAndDispatchNotification() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = new AppointmentLifecycleEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "CONFIRMED", "PENDING", "CONFIRMED", now, now.plusSeconds(3600), null, now
        );
        BookingNotificationPayload expectedPayload = new BookingNotificationPayload(
                event.appointmentId(), event.businessId(), event.customerId(), event.employeeId(),
                "CONFIRMED", "CONFIRMED", "Agendamento confirmado", now
        );
        when(notificationMapper.toResponse(event)).thenReturn(expectedPayload);

        // Act
        BookingNotificationPayload result = sendBookingNotificationUseCase.execute(event);

        // Assert
        assertThat(result).isEqualTo(expectedPayload);
        verify(notificationService).dispatch(expectedPayload);
    }
}

