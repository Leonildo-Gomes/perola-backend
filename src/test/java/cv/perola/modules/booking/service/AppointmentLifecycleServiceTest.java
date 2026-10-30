package cv.perola.modules.booking.service;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.AppointmentHistoryEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentHistoryRepository;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentLifecycleServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentHistoryRepository appointmentHistoryRepository;

    @Mock
    private BusinessEmployeeQueryService businessEmployeeQueryService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentLifecycleService lifecycleService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve gravar histórico append-only, publicar evento e construir resposta com timeline")
    void shouldRecordHistoryPublishEventAndBuildResponse() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-03T10:00:00Z");
        Instant end = Instant.parse("2026-11-03T11:00:00Z");

        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .employeeId(employeeId)
                .serviceId(UUID.randomUUID())
                .createdById(customerId)
                .startTime(start)
                .endTime(end)
                .serviceName("Corte")
                .servicePrice(new BigDecimal("1500.00"))
                .serviceDuration(60)
                .status(AppointmentStatus.CONFIRMED)
                .build();
        appointment.setId(appointmentId);

        AppointmentHistoryEntity historyEntity = AppointmentHistoryEntity.builder()
                .id(UUID.randomUUID())
                .appointmentId(appointmentId)
                .performedById(actorId)
                .action(AppointmentAction.CONFIRMED)
                .fromStatus(AppointmentStatus.PENDING)
                .toStatus(AppointmentStatus.CONFIRMED)
                .build();

        BusinessResponse business = new BusinessResponse(
                businessId, actorId, "Salão", "Santiago", "Praia", "Platô",
                "+2382610000", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now()
        );
        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId, businessId, customerId, employeeId, appointment.getServiceId(),
                "Corte", new BigDecimal("1500.00"), 60, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                null, null, AppointmentStatus.CONFIRMED, "Confirmado", OffsetDateTime.now()
        );

        when(appointmentMapper.toHistoryEntity(
                appointmentId, actorId, AppointmentAction.CONFIRMED,
                AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                null, null, start, end, "Confirmado"
        )).thenReturn(historyEntity);
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
        when(appointmentHistoryRepository.findAllByAppointmentIdOrderByCreatedAtAsc(appointmentId))
                .thenReturn(List.of(historyEntity));
        when(appointmentMapper.toResponse(appointment, List.of(historyEntity), ZoneId.of("Atlantic/Cape_Verde")))
                .thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = lifecycleService.recordTransitionAndBuildResponse(
                appointment, actorId, AppointmentAction.CONFIRMED,
                AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                null, null, start, end, "Confirmado"
        );

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(appointmentHistoryRepository).save(historyEntity);
        verify(eventPublisher).publishEvent(any(AppointmentLifecycleEvent.class));
    }

    @Test
    @DisplayName("Deve bloquear acesso de utilizador terceiro não participante com AccessDeniedException")
    void shouldThrowAccessDeniedWhenUserIsNeitherCustomerNorSalonMember() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(businessId)
                .customerId(UUID.randomUUID())
                .employeeId(employeeId)
                .build();
        appointment.setId(UUID.randomUUID());

        UserPrincipal stranger = new UserPrincipal(
                UUID.randomUUID(), "stranger@perola.cv", "hash", List.of("CUSTOMER"), List.of(), true
        );
        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId))
                .thenReturn(Optional.of(new EmployeeResponse(employeeId, businessId, UUID.randomUUID(), "Carla", true, Instant.now(), Instant.now())));

        // Act & Assert
        assertThatThrownBy(() -> lifecycleService.validateParticipantAccess(appointment, stranger))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Deve bloquear confirmação/conclusão quando X-Business-Id não coincidir com o salão do agendamento")
    void shouldThrowTenantAccessDeniedWhenBusinessIdDiffersFromTenantContext() {
        // Arrange
        TenantContext.setTenantId(UUID.randomUUID());
        AppointmentEntity appointment = AppointmentEntity.builder()
                .businessId(UUID.randomUUID())
                .build();
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "owner@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(TenantContext.getTenantId(), "OWNER")), true
        );

        // Act & Assert
        assertThatThrownBy(() -> lifecycleService.validateSalonManagementAccess(appointment, principal))
                .isInstanceOf(TenantAccessDeniedException.class);
    }
}

