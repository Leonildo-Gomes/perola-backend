package cv.perola.modules.booking.usecase.customer;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.dto.UpdateBusinessCustomerRequest;
import cv.perola.modules.booking.mapper.BusinessCustomerMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import cv.perola.modules.booking.validator.customer.UpdateBusinessCustomerValidator;
import cv.perola.modules.identity.service.CustomerResolutionService;
import cv.perola.modules.identity.service.CustomerSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para atualizar classificação (tier) e notas internas de um cliente no salão.
 */
@Service
@RequiredArgsConstructor
public class UpdateBusinessCustomerUseCase {

    private final BusinessCustomerRepository businessCustomerRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerResolutionService customerResolutionService;
    private final BusinessCustomerMapper businessCustomerMapper;
    private final UpdateBusinessCustomerValidator validator;
    private final AppointmentLifecycleService lifecycleService;

    @Transactional
    public BusinessCustomerResponse execute(UUID businessId, UUID customerId, UpdateBusinessCustomerRequest request) {
        validateTenantAccess(businessId);
        validator.validate(request);

        BusinessCustomerEntity entity = businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de cliente não encontrada para este salão."));

        if (request.tier() != null) {
            entity.setTier(request.tier());
        }
        if (request.internalNotes() != null) {
            entity.setInternalNotes(request.internalNotes());
        }

        BusinessCustomerEntity saved = businessCustomerRepository.save(entity);

        CustomerSummary summary = customerResolutionService.findById(customerId)
                .map(c -> new CustomerSummary(c.getId(), c.getUserId(), c.getName(), c.getEmail(), c.getPhone()))
                .orElse(null);

        List<CustomerAppointmentMetricsProjection> metricsList = appointmentRepository.findMetricsForCustomers(businessId, List.of(customerId));
        CustomerAppointmentMetricsProjection metrics = metricsList.isEmpty() ? null : metricsList.get(0);

        ZoneId businessZone = lifecycleService.resolveBusinessZone(businessId);
        return businessCustomerMapper.toResponse(saved, summary, metrics, businessZone);
    }

    private void validateTenantAccess(UUID businessId) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!tenantId.equals(businessId)) {
            throw new TenantAccessDeniedException("Acesso negado para o salão especificado.");
        }
    }
}
