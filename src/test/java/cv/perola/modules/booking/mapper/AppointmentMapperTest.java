package cv.perola.modules.booking.mapper;

import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.AppointmentHistoryEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentHistoryResponse;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMapperTest {

        private AppointmentMapper appointmentMapper;

        @BeforeEach
        void setUp() {
                appointmentMapper = new AppointmentMapper();
        }

        @Test
        @DisplayName("Deve converter CreateAppointmentRequest e ServiceResponse para AppointmentEntity com snapshots")
        void shouldMapCreateRequestAndServiceSnapshotToEntity() {
                // Arrange
                UUID businessId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                OffsetDateTime startOffset = OffsetDateTime.of(2026, 11, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
                Instant startTime = startOffset.toInstant();
                Instant endTime = startTime.plusSeconds(3600);

                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId,
                                employeeId,
                                serviceId,
                                startOffset,
                                "Corte moderno");
                ServiceResponse service = new ServiceResponse(
                                serviceId,
                                businessId,
                                "Corte & Brushing",
                                "Corte completo",
                                60,
                                new BigDecimal("2500.00"),
                                true,
                                Instant.now(),
                                Instant.now());

                // Act
                AppointmentEntity entity = appointmentMapper.toEntity(
                                request,
                                customerId,
                                customerId,
                                service,
                                startTime,
                                endTime);

                // Assert
                assertThat(entity.getBusinessId()).isEqualTo(businessId);
                assertThat(entity.getCustomerId()).isEqualTo(customerId);
                assertThat(entity.getCreatedById()).isEqualTo(customerId);
                assertThat(entity.getEmployeeId()).isEqualTo(employeeId);
                assertThat(entity.getServiceId()).isEqualTo(serviceId);
                assertThat(entity.getStartTime()).isEqualTo(startTime);
                assertThat(entity.getEndTime()).isEqualTo(endTime);
                assertThat(entity.getServiceName()).isEqualTo("Corte & Brushing");
                assertThat(entity.getServicePrice()).isEqualByComparingTo("2500.00");
                assertThat(entity.getServiceDuration()).isEqualTo(60);
                assertThat(entity.getStatus()).isEqualTo(AppointmentStatus.PENDING);
                assertThat(entity.getNotes()).isEqualTo("Corte moderno");
        }

        @Test
        @DisplayName("Deve converter AppointmentEntity e histórico para AppointmentResponse respeitando o fuso de Cabo Verde (UTC-1)")
        void shouldMapEntityAndHistoryToResponseInCapeVerdeTimezone() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                Instant startTime = Instant.parse("2026-11-03T11:00:00Z");
                Instant endTime = Instant.parse("2026-11-03T12:00:00Z");
                Instant createdAt = Instant.parse("2026-10-30T12:00:00Z");

                AppointmentEntity entity = AppointmentEntity.builder()
                                .businessId(businessId)
                                .customerId(customerId)
                                .employeeId(employeeId)
                                .serviceId(serviceId)
                                .createdById(customerId)
                                .startTime(startTime)
                                .endTime(endTime)
                                .serviceName("Manicure Gel")
                                .servicePrice(new BigDecimal("1800.00"))
                                .serviceDuration(60)
                                .status(AppointmentStatus.PENDING)
                                .notes("Sem alergias")
                                .build();
                entity.setId(appointmentId);
                entity.setCreatedAt(createdAt);

                AppointmentHistoryEntity historyEntity = appointmentMapper.toHistoryEntity(
                                appointmentId,
                                customerId,
                                AppointmentAction.CREATED,
                                null,
                                AppointmentStatus.PENDING,
                                null,
                                null,
                                startTime,
                                endTime,
                                "Criado pelo cliente");
                historyEntity.setId(UUID.randomUUID());
                historyEntity.setCreatedAt(createdAt);

                // Act
                AppointmentResponse response = appointmentMapper.toResponse(
                                entity,
                                List.of(historyEntity),
                                ZoneId.of("Atlantic/Cape_Verde"));

                // Assert
                assertThat(response.id()).isEqualTo(appointmentId);
                assertThat(response.startTime().getOffset()).isEqualTo(ZoneOffset.ofHours(-1));
                assertThat(response.startTime().getHour()).isEqualTo(10);
                assertThat(response.endTime().getHour()).isEqualTo(11);
                assertThat(response.history()).hasSize(1);
                AppointmentHistoryResponse histResp = response.history().getFirst();
                assertThat(histResp.action()).isEqualTo(AppointmentAction.CREATED);
                assertThat(histResp.toStatus()).isEqualTo(AppointmentStatus.PENDING);
                assertThat(histResp.newStartTime().getHour()).isEqualTo(10);
        }

}
