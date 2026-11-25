package cv.perola.modules.notifications.mapper;

import cv.perola.core.config.AppConfig;
import cv.perola.core.config.MailProperties;
import cv.perola.core.shared.event.BookingDomainEvent;
import cv.perola.core.shared.event.BookingEventAction;
import cv.perola.modules.notifications.domain.enums.EmailTemplate;
import cv.perola.modules.notifications.dto.EmailMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mapper dedicado do módulo Notifications para conversão de eventos de domínio
 * em pedidos de envio de e-mail personalizados.
 */
@Component
@RequiredArgsConstructor
public class NotificationMapper {

        private static final Locale PT_CV = Locale.of("pt", "CV");
        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_CV);
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", PT_CV);

        private final MailProperties mailProperties;

        public Optional<EmailMessageRequest> toEmailRequest(BookingDomainEvent event) {
                return resolveTemplate(event.action()).map(template -> {
                        ZoneId zoneId = resolveZoneId(event.businessTimeZone());
                        ZonedDateTime localStart = event.startTime().atZone(zoneId);
                        ZonedDateTime localEnd = event.endTime().atZone(zoneId);

                        String businessName = Objects.requireNonNullElse(event.businessName(), "");
                        String customerName = Objects.requireNonNullElse(event.customerName(), "");

                        Map<String, Object> variables = Map.of(
                                        "customerName", customerName,
                                        "businessName", businessName,
                                        "employeeName", Objects.requireNonNullElse(event.employeeName(), ""),
                                        "serviceName", Objects.requireNonNullElse(event.serviceName(), ""),
                                        "servicePriceCve", Objects.requireNonNullElse(event.servicePriceCve(), ""),
                                        "appointmentDate", DATE_FMT.format(localStart),
                                        "startTime", TIME_FMT.format(localStart),
                                        "endTime", TIME_FMT.format(localEnd),
                                        "notes", Objects.requireNonNullElse(event.notes(), ""),
                                        "myAppointmentsUrl",
                                        Objects.requireNonNullElse(mailProperties.frontendBaseUrl(), "")
                                                        + "/appointments");

                        return EmailMessageRequest.builder()
                                        .to(event.customerEmail())
                                        .recipientName(customerName)
                                        .subject(template.formatSubject(businessName))
                                        .template(template)
                                        .variables(variables)
                                        .build();
                });
        }

        private Optional<EmailTemplate> resolveTemplate(BookingEventAction action) {
                return switch (action) {
                        case CREATED -> Optional.of(EmailTemplate.APPOINTMENT_CREATED);
                        case CONFIRMED, RESCHEDULE_CONFIRMED -> Optional.of(EmailTemplate.APPOINTMENT_CONFIRMED);
                        case CANCELLED -> Optional.of(EmailTemplate.APPOINTMENT_CANCELLED);
                        case RESCHEDULE_REQUESTED -> Optional.of(EmailTemplate.RESCHEDULE_REQUESTED);
                        case RESCHEDULE_REJECTED, CANCEL_REQUESTED, COMPLETED -> Optional.empty();
                };
        }

    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ignored) {
            return ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        }
    }
}
