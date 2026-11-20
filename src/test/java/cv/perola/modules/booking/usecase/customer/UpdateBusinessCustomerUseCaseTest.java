package cv.perola.modules.booking.usecase.customer;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.dto.UpdateBusinessCustomerRequest;
import cv.perola.modules.booking.mapper.BusinessCustomerMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import cv.perola.modules.booking.validator.customer.UpdateBusinessCustomerValidator;
import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.service.CustomerResolutionService;
import cv.perola.modules.identity.service.CustomerSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBusinessCustomerUseCaseTest {

    @Mock
    private BusinessCustomerRepository businessCustomerRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerResolutionService customerResolutionService;

    @Mock
    private BusinessCustomerMapper businessCustomerMapper;

    @Mock
    private UpdateBusinessCustomerValidator validator;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private UpdateBusinessCustomerUseCase useCase;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Cenário de Critério: PATCH alterando tier para VIP e notas internas persiste imediatamente")
    void shouldUpdateTierAndInternalNotesSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
        UUID customerId = UUID.randomUUID();

        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(
                CustomerTier.VIP, "Promovida a VIP após 10 visitas."
        );

        BusinessCustomerEntity existingEntity = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .tier(CustomerTier.REGULAR)
                .internalNotes(null)
                .build();

        when(businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customerId))
                .thenReturn(Optional.of(existingEntity));
        when(businessCustomerRepository.save(existingEntity)).thenReturn(existingEntity);

        CustomerEntity customerEntity = CustomerEntity.builder()
                .name("Márcia Gonçalves")
                .email("marcia@cv.com")
                .phone("+2389912345")
                .userId(UUID.randomUUID())
                .build();
        customerEntity.setId(customerId);
        when(customerResolutionService.findById(customerId)).thenReturn(Optional.of(customerEntity));

        CustomerAppointmentMetricsProjection metrics = mock(CustomerAppointmentMetricsProjection.class);
        when(appointmentRepository.findMetricsForCustomers(businessId, List.of(customerId)))
                .thenReturn(List.of(metrics));

        ZoneId cvZone = ZoneId.of("Atlantic/Cape_Verde");
        when(lifecycleService.resolveBusinessZone(businessId)).thenReturn(cvZone);

        BusinessCustomerResponse response = new BusinessCustomerResponse(
                customerId, "Márcia Gonçalves", "marcia@cv.com", "+2389912345", true,
                CustomerTier.VIP, "Promovida a VIP após 10 visitas.", 10, 10, 0,
                new BigDecimal("30000.00"), OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(businessCustomerMapper.toResponse(eq(existingEntity), any(CustomerSummary.class), eq(metrics), eq(cvZone)))
                .thenReturn(response);

        // Act
        BusinessCustomerResponse result = useCase.execute(businessId, customerId, request);

        // Assert
        assertThat(result.tier()).isEqualTo(CustomerTier.VIP);
        assertThat(result.internalNotes()).isEqualTo("Promovida a VIP após 10 visitas.");
        assertThat(existingEntity.getTier()).isEqualTo(CustomerTier.VIP);
        assertThat(existingEntity.getInternalNotes()).isEqualTo("Promovida a VIP após 10 visitas.");
        verify(businessCustomerRepository).save(existingEntity);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o cliente não for encontrado para o salão")
    void shouldThrowResourceNotFoundExceptionWhenCustomerDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
        UUID customerId = UUID.randomUUID();

        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(CustomerTier.WARNING, "Faltou sem avisar.");
        when(businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId, customerId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ficha de cliente não encontrada");
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando tentar aceder com businessId incompatível com o header")
    void shouldThrowTenantAccessDeniedExceptionWhenBusinessIdDoesNotMatchTenantContext() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        TenantContext.setTenantId(otherTenantId);

        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(CustomerTier.VIP, null);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId, UUID.randomUUID(), request))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Acesso negado");
    }
}
