package cv.perola.modules.booking.usecase;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import org.junit.jupiter.api.AfterEach;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListSalonAppointmentsUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BusinessEmployeeQueryService businessEmployeeQueryService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private ListSalonAppointmentsUseCase listSalonAppointmentsUseCase;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve permitir ao OWNER listar agendamentos do salão")
    void shouldAllowOwnerToListBusinessAppointments() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "owner@perola.cv", "hash",
                List.of("OWNER"), List.of(new TenantMembership(businessId, "OWNER")), true
        );
        ZoneId cvZone = ZoneId.of("Atlantic/Cape_Verde");

        AppointmentEntity entity = AppointmentEntity.builder()
                .businessId(businessId)
                .status(AppointmentStatus.PENDING)
                .build();
        entity.setId(UUID.randomUUID());

        AppointmentResponse mappedResponse = new AppointmentResponse(
                entity.getId(), businessId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Corte", new BigDecimal("1500.00"), 60, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                null, null, AppointmentStatus.PENDING, null, OffsetDateTime.now()
        );

        Page<AppointmentEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        when(lifecycleService.isBusinessOwner(principal, businessId)).thenReturn(true);
        when(lifecycleService.resolveBusinessZone(businessId)).thenReturn(cvZone);
        when(appointmentRepository.findBusinessAppointments(businessId, null, null, null, null, PageRequest.of(0, 20)))
                .thenReturn(page);
        when(appointmentMapper.toResponse(entity, cvZone)).thenReturn(mappedResponse);

        // Act
        PageResponse<AppointmentResponse> result = listSalonAppointmentsUseCase.execute(
                null, null, null, null, 0, 20, principal
        );

        // Assert
        assertThat(result.content()).containsExactly(mappedResponse);
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando EMPLOYEE tentar consultar agenda de outro colaborador")
    void shouldThrowTenantAccessDeniedWhenEmployeeRequestsAnotherEmployeesSchedule() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID ownEmployeeId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);

        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash",
                List.of("EMPLOYEE"), List.of(new TenantMembership(businessId, "EMPLOYEE")), true
        );
        EmployeeResponse ownEmployee = new EmployeeResponse(
                ownEmployeeId, businessId, userId, "Carla", true, Instant.now(), Instant.now()
        );

        when(lifecycleService.isBusinessOwner(principal, businessId)).thenReturn(false);
        when(businessEmployeeQueryService.findEmployeeByBusinessIdAndUserId(businessId, userId))
                .thenReturn(Optional.of(ownEmployee));

        // Act & Assert
        assertThatThrownBy(() -> listSalonAppointmentsUseCase.execute(
                otherEmployeeId, null, null, null, 0, 20, principal
        )).isInstanceOf(TenantAccessDeniedException.class)
                .hasMessage("O colaborador só pode consultar a sua própria agenda.");
    }
}

