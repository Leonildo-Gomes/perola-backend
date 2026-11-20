package cv.perola.modules.booking.usecase.customer;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.mapper.BusinessCustomerMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import cv.perola.modules.identity.service.CustomerResolutionService;
import cv.perola.modules.identity.service.CustomerSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar clientes da carteira do salão com métricas e filtros.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListBusinessCustomersUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final BusinessCustomerRepository businessCustomerRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerResolutionService customerResolutionService;
    private final BusinessCustomerMapper businessCustomerMapper;
    private final AppointmentLifecycleService lifecycleService;

    public PageResponse<BusinessCustomerResponse> execute(
            UUID businessId,
            String search,
            CustomerTier tier,
            int page,
            int size
    ) {
        validateTenantAccess(businessId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Collection<UUID> customerIdsFilter = null;
        if (search != null && !search.trim().isBlank()) {
            List<UUID> matchingIds = customerResolutionService.findCustomerIdsMatching(search.trim());
            if (matchingIds.isEmpty()) {
                return new PageResponse<>(List.of(), safePage, safeSize, 0, 0);
            }
            customerIdsFilter = matchingIds;
        }

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BusinessCustomerEntity> resultPage = businessCustomerRepository.findBusinessCustomers(
                businessId,
                tier,
                customerIdsFilter,
                pageable
        );

        if (resultPage.isEmpty()) {
            return new PageResponse<>(List.of(), resultPage.getNumber(), resultPage.getSize(), 0, resultPage.getTotalPages());
        }

        List<UUID> customerIds = resultPage.getContent().stream()
                .map(BusinessCustomerEntity::getCustomerId)
                .toList();

        Map<UUID, CustomerSummary> summaries = customerResolutionService.findCustomerSummariesByIds(customerIds);

        List<CustomerAppointmentMetricsProjection> metricsList = appointmentRepository.findMetricsForCustomers(businessId, customerIds);
        Map<UUID, CustomerAppointmentMetricsProjection> metricsMap = metricsList.stream()
                .collect(Collectors.toMap(CustomerAppointmentMetricsProjection::getCustomerId, m -> m, (a, b) -> a));

        ZoneId businessZone = lifecycleService.resolveBusinessZone(businessId);

        List<BusinessCustomerResponse> content = resultPage.getContent().stream()
                .map(entity -> businessCustomerMapper.toResponse(
                        entity,
                        summaries.get(entity.getCustomerId()),
                        metricsMap.get(entity.getCustomerId()),
                        businessZone
                ))
                .toList();

        return new PageResponse<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private void validateTenantAccess(UUID businessId) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!tenantId.equals(businessId)) {
            throw new TenantAccessDeniedException("Acesso negado para o salão especificado.");
        }
    }
}
