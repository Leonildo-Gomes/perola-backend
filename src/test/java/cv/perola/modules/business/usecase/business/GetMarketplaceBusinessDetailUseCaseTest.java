package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.MarketplaceBusinessDetailResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMarketplaceBusinessDetailUseCaseTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Spy
    private BusinessMapper businessMapper = new BusinessMapper();

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @InjectMocks
    private GetMarketplaceBusinessDetailUseCase useCase;

    @Test
    @DisplayName("Deve retornar detalhes públicos do salão com serviços e profissionais ativos")
    void shouldReturnBusinessDetailWithServicesAndEmployees() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        BusinessEntity business = BusinessEntity.builder()
                .ownerId(UUID.randomUUID())
                .name("Salão Morabeza")
                .island("São Vicente")
                .city("Mindelo")
                .location("Rua Lisboa")
                .contactPhone("+2382321122")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        business.setId(businessId);

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Penteado Tradicional")
                .durationMinutes(45)
                .price(new BigDecimal("1500.00"))
                .isActive(true)
                .build();
        service.setId(UUID.randomUUID());
        service.setCreatedAt(Instant.now());
        service.setUpdatedAt(Instant.now());

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Nair Silva")
                .isActive(true)
                .build();
        employee.setId(UUID.randomUUID());
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.of(business));
        when(serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of(service));
        when(employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)).thenReturn(List.of(employee));

        // Act
        MarketplaceBusinessDetailResponse response = useCase.execute(businessId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(businessId);
        assertThat(response.name()).isEqualTo("Salão Morabeza");
        assertThat(response.services()).hasSize(1);
        assertThat(response.services().get(0).name()).isEqualTo("Penteado Tradicional");
        assertThat(response.employees()).hasSize(1);
        assertThat(response.employees().get(0).displayName()).isEqualTo("Nair Silva");

        verify(businessRepository).findByIdAndIsActiveTrue(businessId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando salão estiver inativo ou não existir")
    void shouldThrowResourceNotFoundWhenBusinessNotFoundOrInactive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findByIdAndIsActiveTrue(businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(businessId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Salão não encontrado ou inativo");
    }
}
