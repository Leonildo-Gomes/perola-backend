package cv.perola.modules.booking.usecase.customer;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.security.TenantContext;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.BusinessCustomerDetailResponse;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.mapper.BusinessCustomerMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.booking.repository.projection.CustomerAppointmentMetricsProjection;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBusinessCustomerDetailUseCaseTest {

    @Mock
    private BusinessCustomerRepository businessCustomerRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerResolutionService customerResolutionService;

    @Mock
    private BusinessCustomerMapper businessCustomerMapper;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentLifecycleService lifecycleService;

    @InjectMocks
    private GetBusinessCustomerDetailUseCase useCase;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve retornar detalhe da ficha do cliente e marcações recentes com sucesso")
    void shouldReturnBusinessCustomerDetailSuccessfully() {
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
        when(businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customerId))
                .thenReturn(Optional.of(entity));

        cv.perola.modules.identity.domain.CustomerEntity customerEntity = cv.perola.modules.identity.domain.CustomerEntity.builder()
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

        BusinessCustomerResponse customerResponse = new BusinessCustomerResponse(
                customerId, "Márcia Gonçalves", "marcia@cv.com", "+2389912345", true,
                CustomerTier.VIP, "Cliente VIP.", 5, 5, 0, new BigDecimal("15000.00"),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(businessCustomerMapper.toResponse(eq(entity), any(CustomerSummary.class), eq(metrics), eq(cvZone)))
                .thenReturn(customerResponse);

        AppointmentEntity appointmentEntity = AppointmentEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .build();
        when(appointmentRepository.findTop10ByBusinessIdAndCustomerIdOrderByStartTimeDesc(businessId, customerId))
                .thenReturn(List.of(appointmentEntity));

        AppointmentResponse appResponse = new AppointmentResponse(
                UUID.randomUUID(), businessId, customerId, UUID.randomUUID(), UUID.randomUUID(),
                "Corte", new BigDecimal("1500.00"), 60, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                null, null, cv.perola.modules.booking.domain.enums.AppointmentStatus.COMPLETED,
                null, OffsetDateTime.now()
        );
        when(appointmentMapper.toResponse(appointmentEntity, cvZone)).thenReturn(appResponse);

        // Act
        BusinessCustomerDetailResponse result = useCase.execute(businessId, customerId);

        // Assert
        assertThat(result.customer()).isEqualTo(customerResponse);
        assertThat(result.recentAppointments()).containsExactly(appResponse);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o cliente não tiver ficha no salão")
    void shouldThrowResourceNotFoundExceptionWhenCustomerNotFoundInBusiness() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
        UUID customerId = UUID.randomUUID();

        when(businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId, customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ficha de cliente não encontrada");
    }
}
