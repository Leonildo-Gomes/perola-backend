package cv.perola.modules.booking.mapper;

import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.identity.service.CustomerSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessCustomerMapperTest {

    private final BusinessCustomerMapper mapper = new BusinessCustomerMapper();

    @Test
    @DisplayName("Deve mapear BusinessCustomerEntity, CustomerSummary e CustomerAppointmentMetricsProjection com sucesso")
    void shouldMapToBusinessCustomerResponseCorrectly() {
        UUID customerId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        ZoneId cvZone = ZoneId.of("Atlantic/Cape_Verde");

        BusinessCustomerEntity entity = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .tier(CustomerTier.VIP)
                .internalNotes("Cliente muito exigente.")
                .build();
        entity.setCreatedAt(Instant.parse("2026-03-12T09:30:00Z"));

        CustomerSummary summary = new CustomerSummary(
                customerId, UUID.randomUUID(), "Márcia Gonçalves", "marcia.goncalves@exemplo.cv", "+2389912345"
        );

        CustomerAppointmentMetricsProjection metrics = mock(CustomerAppointmentMetricsProjection.class);
        when(metrics.getCustomerId()).thenReturn(customerId);
        when(metrics.getTotalAppointments()).thenReturn(14L);
        when(metrics.getCompletedAppointments()).thenReturn(13L);
        when(metrics.getCancelledAppointments()).thenReturn(1L);
        when(metrics.getTotalSpentCve()).thenReturn(new BigDecimal("32500.00"));
        when(metrics.getLastAppointmentAt()).thenReturn(Instant.parse("2026-11-10T15:00:00Z"));

        BusinessCustomerResponse response = mapper.toResponse(entity, summary, metrics, cvZone);

        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.name()).isEqualTo("Márcia Gonçalves");
        assertThat(response.email()).isEqualTo("marcia.goncalves@exemplo.cv");
        assertThat(response.phone()).isEqualTo("+2389912345");
        assertThat(response.hasRegisteredAccount()).isTrue();
        assertThat(response.tier()).isEqualTo(CustomerTier.VIP);
        assertThat(response.internalNotes()).isEqualTo("Cliente muito exigente.");
        assertThat(response.totalAppointments()).isEqualTo(14L);
        assertThat(response.completedAppointments()).isEqualTo(13L);
        assertThat(response.cancelledAppointments()).isEqualTo(1L);
        assertThat(response.totalSpentCve()).isEqualByComparingTo("32500.00");
        assertThat(response.lastAppointmentAt()).isNotNull();
        assertThat(response.createdAt()).isNotNull();
    }
}
