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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBusinessCustomersUseCaseTest {

    @Mock
    private BusinessCustomerRepository businessCustomerRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerResolutionService customerResolutionService;

    @Mock
    private BusinessCustomerMapper businessCustomerMapper;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private ListBusinessCustomersUseCase useCase;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve listar clientes da carteira com sucesso e enriquecer com métricas e dados de contacto")
    void shouldListBusinessCustomersSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);

        UUID customerId = UUID.randomUUID();
        BusinessCustomerEntity entity = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .tier(CustomerTier.VIP)
                .internalNotes("Cliente VIP.")
                .build();

        Page<BusinessCustomerEntity> page = new PageImpl<>(List.of(entity));
        when(businessCustomerRepository.findBusinessCustomers(eq(businessId), eq(CustomerTier.VIP), eq(null), any(Pageable.class)))
                .thenReturn(page);

        CustomerSummary summary = new CustomerSummary(customerId, UUID.randomUUID(), "Márcia", "marcia@cv.com", "+2389912345");
        when(customerResolutionService.findCustomerSummariesByIds(List.of(customerId)))
                .thenReturn(Map.of(customerId, summary));

        CustomerAppointmentMetricsProjection metrics = mock(CustomerAppointmentMetricsProjection.class);
        when(metrics.getCustomerId()).thenReturn(customerId);
        when(appointmentRepository.findMetricsForCustomers(businessId, List.of(customerId)))
                .thenReturn(List.of(metrics));

        ZoneId cvZone = ZoneId.of("Atlantic/Cape_Verde");
        when(lifecycleService.resolveBusinessZone(businessId)).thenReturn(cvZone);

        BusinessCustomerResponse mappedResponse = new BusinessCustomerResponse(
                customerId, "Márcia", "marcia@cv.com", "+2389912345", true,
                CustomerTier.VIP, "Cliente VIP.", 10, 9, 1, new BigDecimal("20000.00"),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(businessCustomerMapper.toResponse(entity, summary, metrics, cvZone))
                .thenReturn(mappedResponse);

        // Act
        PageResponse<BusinessCustomerResponse> result = useCase.execute(businessId, null, CustomerTier.VIP, 0, 20);

        // Assert
        assertThat(result.content()).containsExactly(mappedResponse);
        verify(businessCustomerRepository).findBusinessCustomers(eq(businessId), eq(CustomerTier.VIP), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve filtrar clientes por busca textual utilizando findCustomerIdsMatching")
    void shouldFilterCustomersBySearchQuery() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);

        UUID customerId = UUID.randomUUID();
        when(customerResolutionService.findCustomerIdsMatching("marcia"))
                .thenReturn(List.of(customerId));

        BusinessCustomerEntity entity = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .tier(CustomerTier.NEW)
                .build();

        Page<BusinessCustomerEntity> page = new PageImpl<>(List.of(entity));
        when(businessCustomerRepository.findBusinessCustomers(eq(businessId), eq(null), eq(List.of(customerId)), any(Pageable.class)))
                .thenReturn(page);

        CustomerSummary summary = new CustomerSummary(customerId, null, "Márcia", "marcia@cv.com", "+2389912345");
        when(customerResolutionService.findCustomerSummariesByIds(List.of(customerId)))
                .thenReturn(Map.of(customerId, summary));
        when(appointmentRepository.findMetricsForCustomers(businessId, List.of(customerId)))
                .thenReturn(List.of());
        when(lifecycleService.resolveBusinessZone(businessId)).thenReturn(ZoneId.of("Atlantic/Cape_Verde"));

        BusinessCustomerResponse mappedResponse = new BusinessCustomerResponse(
                customerId, "Márcia", "marcia@cv.com", "+2389912345", false,
                CustomerTier.NEW, null, 0, 0, 0, BigDecimal.ZERO, null, null
        );
        when(businessCustomerMapper.toResponse(eq(entity), eq(summary), any(), any(ZoneId.class)))
                .thenReturn(mappedResponse);

        // Act
        PageResponse<BusinessCustomerResponse> result = useCase.execute(businessId, "marcia", null, 0, 20);

        // Assert
        assertThat(result.content()).containsExactly(mappedResponse);
    }

    @Test
    @DisplayName("Deve retornar página vazia imediatamente quando a busca textual não encontrar nenhum cliente")
    void shouldReturnEmptyPageWhenSearchMatchesNoCustomers() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);

        when(customerResolutionService.findCustomerIdsMatching("inexistente"))
                .thenReturn(List.of());

        // Act
        PageResponse<BusinessCustomerResponse> result = useCase.execute(businessId, "inexistente", null, 0, 20);

        // Assert
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando businessId não coincidir com o tenant ativo")
    void shouldThrowTenantAccessDeniedExceptionWhenBusinessIdDoesNotMatchActiveTenant() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID otherBusinessId = UUID.randomUUID();
        TenantContext.setTenantId(otherBusinessId);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId, null, null, 0, 20))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Acesso negado");
    }
}
