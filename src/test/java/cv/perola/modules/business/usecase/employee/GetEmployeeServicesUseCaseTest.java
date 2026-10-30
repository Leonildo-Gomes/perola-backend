package cv.perola.modules.business.usecase.employee;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.EmployeeServicesResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository;
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
class GetEmployeeServicesUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private EmployeeServiceRepository employeeServiceRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private GetEmployeeServicesUseCase getEmployeeServicesUseCase;

    @Test
    @DisplayName("Deve retornar os serviços atribuídos ao colaborador")
    void shouldReturnAssignedServicesForEmployee() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        employee.setId(employeeId);

        EmployeeServiceEntity relation = EmployeeServiceEntity.builder()
                .businessId(businessId)
                .employeeId(employeeId)
                .serviceId(serviceId)
                .build();

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Manicure")
                .durationMinutes(40)
                .price(new BigDecimal("1200.00"))
                .isActive(true)
                .build();
        service.setId(serviceId);
        service.setCreatedAt(Instant.now());
        service.setUpdatedAt(Instant.now());

        when(employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(employeeServiceRepository.findAllByBusinessIdAndEmployeeId(businessId, employeeId)).thenReturn(List.of(relation));
        when(serviceRepository.findAllByBusinessIdAndIdIn(businessId, List.of(serviceId))).thenReturn(List.of(service));

        // Act
        EmployeeServicesResponse response = getEmployeeServicesUseCase.execute(businessId, employeeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.businessId()).isEqualTo(businessId);
        assertThat(response.services()).hasSize(1);
        assertThat(response.services().get(0).name()).isEqualTo("Manicure");

        verify(employeeServiceRepository).findAllByBusinessIdAndEmployeeId(businessId, employeeId);
    }

    @Test
    @DisplayName("Deve retornar lista vazia de serviços quando colaborador não tiver serviços atribuídos")
    void shouldReturnEmptyListWhenNoServicesAssigned() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        employee.setId(employeeId);

        when(employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(employeeServiceRepository.findAllByBusinessIdAndEmployeeId(businessId, employeeId)).thenReturn(List.of());

        // Act
        EmployeeServicesResponse response = getEmployeeServicesUseCase.execute(businessId, employeeId);

        // Assert
        assertThat(response.services()).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando profissional não for encontrado")
    void shouldThrowResourceNotFoundWhenEmployeeDoesNotExist() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getEmployeeServicesUseCase.execute(businessId, employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profissional não encontrado ou inativo");
    }
}
