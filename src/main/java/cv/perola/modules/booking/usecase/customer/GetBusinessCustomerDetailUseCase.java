package cv.perola.modules.booking.usecase.customer;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.BusinessCustomerDetailResponse;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.mapper.BusinessCustomerMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import cv.perola.modules.identity.service.CustomerResolutionService;
import cv.perola.modules.identity.service.CustomerSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para obter o detalhe da ficha do cliente no salão acompanhado do
 * histórico recente.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBusinessCustomerDetailUseCase {

    private final BusinessCustomerRepository businessCustomerRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerResolutionService customerResolutionService;
    private final BusinessCustomerMapper businessCustomerMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentLifecycleService lifecycleService;

    public BusinessCustomerDetailResponse execute(UUID businessId, UUID customerId) {
        validateTenantAccess(businessId);

        BusinessCustomerEntity entity = businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de cliente não encontrada para este salão."));

        CustomerSummary summary = customerResolutionService.findById(customerId)
                .map(c -> new CustomerSummary(c.getId(), c.getUserId(), c.getName(), c.getEmail(), c.getPhone()))
                .orElse(null);

        List<CustomerAppointmentMetricsProjection> metricsList = appointmentRepository
                .findMetricsForCustomers(businessId, List.of(customerId));
        CustomerAppointmentMetricsProjection metrics = metricsList.isEmpty() ? null : metricsList.get(0);

        ZoneId businessZone = lifecycleService.resolveBusinessZone(businessId);
        BusinessCustomerResponse customerResponse = businessCustomerMapper.toResponse(entity, summary, metrics,
                businessZone);

        List<AppointmentEntity> recentEntities = appointmentRepository
                .findTop10ByBusinessIdAndCustomerIdOrderByStartTimeDesc(businessId, customerId);

        List<AppointmentResponse> recentAppointments = recentEntities.stream()
                .map(app -> appointmentMapper.toResponse(app, businessZone))
                .toList();

        return new BusinessCustomerDetailResponse(customerResponse, recentAppointments);
    }

    private void validateTenantAccess(UUID businessId) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!tenantId.equals(businessId)) {
            throw new TenantAccessDeniedException("Acesso negado para o salão especificado.");
        }
    }
}
