package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para listar a agenda de marcações do salão (requer
 * X-Business-Id).
 * Proprietários (OWNER) visualizam toda a agenda ou filtram por colaborador;
 * colaboradores (EMPLOYEE) visualizam apenas a sua própria agenda.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListSalonAppointmentsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final AppointmentRepository appointmentRepository;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentLifecycleService lifecycleService;

    public PageResponse<AppointmentResponse> execute(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate,
            AppointmentStatus status,
            int page,
            int size,
            UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();
        UUID effectiveEmployeeId = resolveEffectiveEmployeeId(businessId, employeeId, principal);

        ZoneId businessZone = lifecycleService.resolveBusinessZone(businessId);
        Instant startFrom = startDate != null ? startDate.atStartOfDay(businessZone).toInstant() : null;
        Instant startTo = endDate != null ? endDate.plusDays(1).atStartOfDay(businessZone).toInstant() : null;

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<AppointmentEntity> resultPage = appointmentRepository.findBusinessAppointments(
                businessId,
                effectiveEmployeeId,
                status,
                startFrom,
                startTo,
                PageRequest.of(safePage, safeSize));

        List<AppointmentResponse> content = resultPage.getContent().stream()
                .map(entity -> appointmentMapper.toResponse(entity, businessZone))
                .toList();

        return new PageResponse<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages());
    }

    private UUID resolveEffectiveEmployeeId(UUID businessId, UUID requestedEmployeeId, UserPrincipal principal) {
        if (lifecycleService.isBusinessOwner(principal, businessId)) {
            return requestedEmployeeId;
        }

        EmployeeResponse ownEmployee = businessEmployeeQueryService
                .findEmployeeByBusinessIdAndUserId(businessId, principal.getId())
                .orElseThrow(
                        () -> new TenantAccessDeniedException("Perfil de colaborador não encontrado no salão ativo."));

        if (requestedEmployeeId != null && !requestedEmployeeId.equals(ownEmployee.id())) {
            throw new TenantAccessDeniedException("O colaborador só pode consultar a sua própria agenda.");
        }

        return ownEmployee.id();
    }
}
