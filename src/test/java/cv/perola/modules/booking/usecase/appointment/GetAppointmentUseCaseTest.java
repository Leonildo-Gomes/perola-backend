package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAppointmentUseCaseTest {

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private GetAppointmentUseCase getAppointmentUseCase;

    @Test
    @DisplayName("Deve validar acesso e retornar AppointmentResponse com linha do tempo de auditoria")
    void shouldValidateAccessAndReturnAppointmentWithHistory() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "cust@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true);

        AppointmentEntity appointment = AppointmentEntity.builder()
                .customerId(userId)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        appointment.setId(appointmentId);

        AppointmentResponse expected = new AppointmentResponse(
                appointmentId, UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(),
                "Corte", new BigDecimal("1500.00"), 60, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                null, null, AppointmentStatus.CONFIRMED, null, OffsetDateTime.now()
        );

        when(lifecycleService.findAppointmentOrThrow(appointmentId)).thenReturn(appointment);
        when(lifecycleService.buildResponseWithHistory(appointment)).thenReturn(expected);

        // Act
        AppointmentResponse result = getAppointmentUseCase.execute(appointmentId, principal);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(lifecycleService).validateParticipantAccess(appointment, principal);
        verify(lifecycleService).buildResponseWithHistory(appointment);
    }
}

