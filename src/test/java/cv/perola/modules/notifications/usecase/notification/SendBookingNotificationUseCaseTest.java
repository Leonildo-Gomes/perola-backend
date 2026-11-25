package cv.perola.modules.notifications.usecase.notification;

import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.core.shared.event.BookingEventAction;
import cv.perola.modules.notifications.domain.enums.EmailTemplate;
import cv.perola.modules.notifications.dto.EmailMessageRequest;
import cv.perola.modules.notifications.mapper.NotificationMapper;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @DisplayName("Deve mapear o evento de domínio e enviar o e-mail quando existir template correspondente")
    void shouldMapEventAndSendEmailWhenTemplateIsPresent() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(UUID.randomUUID())
                .businessId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .performedById(UUID.randomUUID())
                .action(BookingEventAction.CONFIRMED)
                .fromStatus("PENDING")
                .toStatus("CONFIRMED")
                .startTime(now)
                .endTime(now.plusSeconds(3600))
                .occurredAt(now)
                .customerEmail("cliente@perola.cv")
                .build();

        EmailMessageRequest expectedRequest = EmailMessageRequest.builder()
                .to("cliente@perola.cv")
                .recipientName("Ana")
                .subject("Marcação confirmada em Salão Pérola")
                .template(EmailTemplate.APPOINTMENT_CONFIRMED)
                .variables(Map.of())
                .build();

        when(notificationMapper.toEmailRequest(event)).thenReturn(Optional.of(expectedRequest));

        // Act
        sendBookingNotificationUseCase.execute(event);

        // Assert
        verify(notificationService).send(expectedRequest);
    }

    @Test
    @DisplayName("Não deve chamar NotificationService quando a ação não possuir template de e-mail")
    void shouldNotSendEmailWhenMapperReturnsEmpty() {
        // Arrange
        Instant now = Instant.now();
        AppointmentLifecycleEvent event = AppointmentLifecycleEvent.builder()
                .appointmentId(UUID.randomUUID())
                .action(BookingEventAction.COMPLETED)
                .startTime(now)
                .endTime(now.plusSeconds(3600))
                .build();

        when(notificationMapper.toEmailRequest(event)).thenReturn(Optional.empty());

        // Act
        sendBookingNotificationUseCase.execute(event);

        // Assert
        verify(notificationService, never()).send(any());
    }
}
