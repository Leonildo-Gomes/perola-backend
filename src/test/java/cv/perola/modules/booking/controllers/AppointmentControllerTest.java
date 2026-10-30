package cv.perola.modules.booking.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.usecase.CreateAppointmentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

        @Mock
        private CreateAppointmentUseCase createAppointmentUseCase;

        @InjectMocks
        private AppointmentController appointmentController;

        @Test
        @DisplayName("Deve retornar HTTP 201 Created ao criar agendamento com utilizador autenticado")
        void shouldReturnCreatedWhenAppointmentIsCreated() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UserPrincipal principal = new UserPrincipal(
                                userId,
                                "cliente@perola.cv",
                                "hash",
                                List.of("CUSTOMER"),
                                List.of(),
                                true);
                OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                startTime,
                                "Notas");
                AppointmentResponse expected = new AppointmentResponse(
                                UUID.randomUUID(),
                                request.businessId(),
                                userId,
                                request.employeeId(),
                                request.serviceId(),
                                "Corte",
                                new BigDecimal("1500.00"),
                                45,
                                startTime,
                                startTime.plusMinutes(45),
                                null,
                                null,
                                AppointmentStatus.PENDING,
                                "Notas",
                                startTime);
                when(createAppointmentUseCase.execute(userId, request)).thenReturn(expected);

                // Act
                ResponseEntity<AppointmentResponse> response = appointmentController.createAppointment(principal,
                                request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isEqualTo(expected);
                verify(createAppointmentUseCase).execute(userId, request);
        }

        @Test
        @DisplayName("Deve lançar AuthenticationFailedException quando o principal for nulo")
        void shouldThrowAuthenticationFailedExceptionWhenPrincipalIsNull() {
                // Arrange
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)),
                                null);

                // Act & Assert
                assertThatThrownBy(() -> appointmentController.createAppointment(null, request))
                                .isInstanceOf(AuthenticationFailedException.class)
                                .hasMessage("Autenticação necessária.");
        }
}