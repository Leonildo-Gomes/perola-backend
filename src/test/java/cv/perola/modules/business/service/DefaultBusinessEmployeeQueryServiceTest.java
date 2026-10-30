package cv.perola.modules.business.service;

import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBusinessEmployeeQueryServiceTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private EmployeeServiceRepository employeeServiceRepository;

    @Mock
    private BusinessMapper businessMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private DefaultBusinessEmployeeQueryService employeeQueryService;

    @Test
    @DisplayName("Deve retornar BusinessResponse quando salão encontrado por ID")
    void shouldReturnBusinessResponseWhenBusinessFoundById() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        BusinessEntity entity = BusinessEntity.builder()
                .name("Salão Pérola")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        BusinessResponse expectedResponse = new BusinessResponse(
                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                "Platô", "+2389911223", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now()
        );
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));
        when(businessMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<BusinessResponse> result = employeeQueryService.findBusinessById(businessId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(businessRepository).findById(businessId);
    }

    @Test
    @DisplayName("Deve retornar EmployeeResponse quando colaborador encontrado por ID")
    void shouldReturnEmployeeResponseWhenEmployeeFoundById() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        EmployeeEntity entity = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Carlos")
                .isActive(true)
                .build();
        EmployeeResponse expectedResponse = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeById(employeeId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(employeeRepository).findById(employeeId);
    }

    @Test
    @DisplayName("Deve retornar vazio quando colaborador não encontrado por ID")
    void shouldReturnEmptyWhenEmployeeNotFoundById() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeById(employeeId);

        // Assert
        assertThat(result).isEmpty();
        verify(employeeRepository).findById(employeeId);
    }

    @Test
    @DisplayName("Deve retornar EmployeeResponse quando colaborador encontrado por ID e businessId")
    void shouldReturnEmployeeResponseWhenFoundByIdAndBusinessId() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        EmployeeEntity entity = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Ana")
                .isActive(true)
                .build();
        EmployeeResponse expectedResponse = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Ana", true, Instant.now(), Instant.now()
        );
        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(employeeRepository).findByIdAndBusinessId(employeeId, businessId);
    }

    @Test
    @DisplayName("Deve retornar ServiceResponse quando serviço encontrado por ID e businessId")
    void shouldReturnServiceResponseWhenFoundByIdAndBusinessId() {
        // Arrange
        UUID serviceId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        ServiceEntity entity = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte")
                .durationMinutes(60)
                .price(new BigDecimal("1500.00"))
                .isActive(true)
                .build();
        ServiceResponse expectedResponse = new ServiceResponse(
                serviceId, businessId, "Corte", null, 60, new BigDecimal("1500.00"), true, Instant.now(), Instant.now()
        );
        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(entity));
        when(serviceMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<ServiceResponse> result = employeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(serviceRepository).findByIdAndBusinessId(serviceId, businessId);
    }

    @Test
    @DisplayName("Deve verificar corretamente se colaborador possui serviços atribuídos")
    void shouldCheckIfEmployeeHasAssignedServices() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        EmployeeServiceEntity assignment = EmployeeServiceEntity.builder()
                .employeeId(employeeId)
                .serviceId(serviceId)
                .businessId(businessId)
                .build();
        when(employeeServiceRepository.findAllByBusinessIdAndEmployeeId(businessId, employeeId))
                .thenReturn(List.of(assignment));
        when(employeeServiceRepository.existsByBusinessIdAndEmployeeIdAndServiceId(businessId, employeeId, serviceId))
                .thenReturn(true);

        // Act
        boolean hasAny = employeeQueryService.hasAnyAssignedServices(businessId, employeeId);
        boolean isAssigned = employeeQueryService.isServiceAssignedToEmployee(businessId, employeeId, serviceId);

        // Assert
        assertThat(hasAny).isTrue();
        assertThat(isAssigned).isTrue();
    }

    @Test
    @DisplayName("Deve retornar EmployeeResponse quando colaborador encontrado por businessId e userId")
    void shouldReturnEmployeeResponseWhenFoundByBusinessIdAndUserId() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity entity = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(userId)
                .displayName("Carla")
                .isActive(true)
                .build();
        EmployeeResponse expectedResponse = new EmployeeResponse(
                employeeId, businessId, userId, "Carla", true, Instant.now(), Instant.now()
        );
        when(employeeRepository.findByBusinessIdAndUserId(businessId, userId)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeByBusinessIdAndUserId(businessId, userId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(employeeRepository).findByBusinessIdAndUserId(businessId, userId);
    }
}
