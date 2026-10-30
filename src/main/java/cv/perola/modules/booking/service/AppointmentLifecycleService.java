package cv.perola.modules.booking.service;

import cv.perola.core.config.AppConfig;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
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
import cv.perola.modules.identity.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de suporte ao ciclo de vida de agendamentos: resolução de permissões,
 * registo de auditoria imutável (append-only) e publicação de eventos de domínio.
 */
@Service
@RequiredArgsConstructor
public class AppointmentLifecycleService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentEntity findAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }

    public void validateSalonManagementAccess(AppointmentEntity appointment, UserPrincipal principal) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!tenantId.equals(appointment.getBusinessId())) {
            throw new TenantAccessDeniedException("O agendamento não pertence ao salão especificado em X-Business-Id.");
        }

        if (isBusinessOwner(principal, tenantId)) {
            return;
        }

        EmployeeResponse employee = businessEmployeeQueryService
                .findEmployeeByIdAndBusinessId(appointment.getEmployeeId(), tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Profissional do agendamento não encontrado no salão ativo."));

        boolean isAssignedEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isAssignedEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode gerir os seus próprios agendamentos.");
        }
    }

    public void validateParticipantAccess(AppointmentEntity appointment, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Autenticação necessária.");
        }

        if (appointment.getCustomerId().equals(principal.getId())) {
            return;
        }

        UUID businessId = appointment.getBusinessId();
        if (isBusinessOwner(principal, businessId)) {
            return;
        }

        boolean isAssignedEmployee = businessEmployeeQueryService
                .findEmployeeByIdAndBusinessId(appointment.getEmployeeId(), businessId)
                .map(emp -> emp.userId() != null && emp.userId().equals(principal.getId()))
                .orElse(false);

        if (!isAssignedEmployee) {
            throw new AccessDeniedException("Não tem permissão para aceder ou alterar este agendamento.");
        }
    }

    public boolean isBusinessOwner(UserPrincipal principal, UUID businessId) {
        if (principal == null || principal.getMemberships() == null) {
            return false;
        }
        return principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));
    }

    public AppointmentResponse recordTransitionAndBuildResponse(
            AppointmentEntity savedAppointment,
            UUID performedById,
            AppointmentAction action,
            AppointmentStatus fromStatus,
            AppointmentStatus toStatus,
            Instant oldStartTime,
            Instant oldEndTime,
            Instant newStartTime,
            Instant newEndTime,
            String notes
    ) {
        AppointmentHistoryEntity historyEntry = appointmentMapper.toHistoryEntity(
                savedAppointment.getId(),
                performedById,
                action,
                fromStatus,
                toStatus,
                oldStartTime,
                oldEndTime,
                newStartTime,
                newEndTime,
                notes
        );
        appointmentHistoryRepository.save(historyEntry);

        eventPublisher.publishEvent(new AppointmentLifecycleEvent(
                savedAppointment.getId(),
                savedAppointment.getBusinessId(),
                savedAppointment.getCustomerId(),
                savedAppointment.getEmployeeId(),
                performedById,
                action.name(),
                fromStatus != null ? fromStatus.name() : null,
                toStatus.name(),
                savedAppointment.getStartTime(),
                savedAppointment.getEndTime(),
                notes,
                Instant.now()
        ));

        return buildResponseWithHistory(savedAppointment);
    }

    public AppointmentResponse buildResponseWithHistory(AppointmentEntity appointment) {
        ZoneId businessZone = resolveBusinessZone(appointment.getBusinessId());
        List<AppointmentHistoryEntity> history = appointmentHistoryRepository
                .findAllByAppointmentIdOrderByCreatedAtAsc(appointment.getId());
        return appointmentMapper.toResponse(appointment, history, businessZone);
    }

    public ZoneId resolveBusinessZone(UUID businessId) {
        return businessEmployeeQueryService.findBusinessById(businessId)
                .map(BusinessResponse::timezone)
                .map(this::parseZoneId)
                .orElse(ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE));
    }

    private ZoneId parseZoneId(String timezone) {
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

