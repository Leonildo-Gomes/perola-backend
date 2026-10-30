package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyAppointmentsUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private ListMyAppointmentsUseCase listMyAppointmentsUseCase;

    @Test
    @DisplayName("Deve retornar página de agendamentos do cliente autenticado")
    void shouldReturnPaginatedCustomerAppointments() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        ZoneId cvZone = ZoneId.of("Atlantic/Cape_Verde");

        AppointmentEntity entity = AppointmentEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        entity.setId(appointmentId);

        AppointmentResponse mappedResponse = new AppointmentResponse(
                appointmentId, businessId, customerId, UUID.randomUUID(), UUID.randomUUID(),
                "Corte", new BigDecimal("1500.00"), 60, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                null, null, AppointmentStatus.CONFIRMED, null, OffsetDateTime.now()
        );

        Page<AppointmentEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        when(appointmentRepository.findCustomerAppointments(customerId, AppointmentStatus.CONFIRMED, PageRequest.of(0, 20)))
                .thenReturn(page);
        when(lifecycleService.resolveBusinessZone(businessId)).thenReturn(cvZone);
        when(appointmentMapper.toResponse(entity, cvZone)).thenReturn(mappedResponse);

        // Act
        PageResponse<AppointmentResponse> result = listMyAppointmentsUseCase.execute(
                customerId, AppointmentStatus.CONFIRMED, 0, 20
        );

        // Assert
        assertThat(result.content()).containsExactly(mappedResponse);
        assertThat(result.totalElements()).isEqualTo(1);
    }
}

