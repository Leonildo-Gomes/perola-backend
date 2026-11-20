package cv.perola.modules.booking.mapper;

import cv.perola.core.config.AppConfig;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.identity.service.CustomerSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class BusinessCustomerMapper {

    public BusinessCustomerResponse toResponse(
            BusinessCustomerEntity entity,
            CustomerSummary summary,
            CustomerAppointmentMetricsProjection metrics,
            ZoneId zoneId
    ) {
        ZoneId targetZone = zoneId != null ? zoneId : ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);

        String name = summary != null ? summary.name() : null;
        String email = summary != null ? summary.email() : null;
        String phone = summary != null ? summary.phone() : null;
        boolean hasRegisteredAccount = summary != null && summary.hasRegisteredAccount();

        long totalAppointments = metrics != null ? metrics.getTotalAppointments() : 0L;
        long completedAppointments = metrics != null ? metrics.getCompletedAppointments() : 0L;
        long cancelledAppointments = metrics != null ? metrics.getCancelledAppointments() : 0L;
        BigDecimal totalSpent = metrics != null && metrics.getTotalSpentCve() != null
                ? metrics.getTotalSpentCve()
                : BigDecimal.ZERO;
        Instant lastAppInstant = metrics != null ? metrics.getLastAppointmentAt() : null;

        return new BusinessCustomerResponse(
                entity.getCustomerId(),
                name,
                email,
                phone,
                hasRegisteredAccount,
                entity.getTier(),
                entity.getInternalNotes(),
                totalAppointments,
                completedAppointments,
                cancelledAppointments,
                totalSpent,
                lastAppInstant != null ? OffsetDateTime.ofInstant(lastAppInstant, targetZone) : null,
                entity.getCreatedAt() != null ? OffsetDateTime.ofInstant(entity.getCreatedAt(), targetZone) : null
        );
    }
}
