package cv.perola.modules.business.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.AssignEmployeeServicesRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignEmployeeServicesUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private EmployeeServiceRepository employeeServiceRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private AssignEmployeeServicesUseCase assignEmployeeServicesUseCase;

    @Test
    @DisplayName("Deve atribuir serviços ao profissional e substituir vínculos anteriores com sucesso")
    void shouldAssignServicesToEmployeeSuccessfully() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID s1Id = UUID.randomUUID();
        UUID s2Id = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        employee.setId(employeeId);

        ServiceEntity s1 = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Feminino")
                .durationMinutes(45)
                .price(new BigDecimal("1500.00"))
                .isActive(true)
                .build();
        s1.setId(s1Id);
        s1.setCreatedAt(Instant.now());
        s1.setUpdatedAt(Instant.now());

        ServiceEntity s2 = ServiceEntity.builder()
                .businessId(businessId)
                .name("Brushing")
                .durationMinutes(30)
                .price(new BigDecimal("1000.00"))
                .isActive(true)
                .build();
        s2.setId(s2Id);
        s2.setCreatedAt(Instant.now());
        s2.setUpdatedAt(Instant.now());

        AssignEmployeeServicesRequest request = new AssignEmployeeServicesRequest(List.of(s1Id, s2Id));

        when(employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(serviceRepository.findAllByBusinessIdAndIdIn(businessId, List.of(s1Id, s2Id))).thenReturn(List.of(s1, s2));

        // Act
        EmployeeServicesResponse response = assignEmployeeServicesUseCase.execute(businessId, employeeId, request);

        // Assert
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.businessId()).isEqualTo(businessId);
        assertThat(response.services()).hasSize(2);

        verify(employeeServiceRepository).deleteAllByBusinessIdAndEmployeeId(businessId, employeeId);
        verify(employeeServiceRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando profissional não existir ou estiver inativo")
    void shouldThrowResourceNotFoundWhenEmployeeDoesNotExistOrIsInactive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        AssignEmployeeServicesRequest request = new AssignEmployeeServicesRequest(List.of(UUID.randomUUID()));

        when(employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assignEmployeeServicesUseCase.execute(businessId, employeeId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profissional não encontrado ou inativo");

        verify(employeeServiceRepository, never()).deleteAllByBusinessIdAndEmployeeId(any(), any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando algum serviço não pertencer ao salão")
    void shouldThrowResourceNotFoundWhenServiceDoesNotBelongToBusiness() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID validServiceId = UUID.randomUUID();
        UUID foreignServiceId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        employee.setId(employeeId);

        ServiceEntity validService = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Feminino")
                .durationMinutes(45)
                .price(new BigDecimal("1500.00"))
                .isActive(true)
                .build();
        validService.setId(validServiceId);

        AssignEmployeeServicesRequest request = new AssignEmployeeServicesRequest(List.of(validServiceId, foreignServiceId));

        when(employeeRepository.findByIdAndBusinessIdAndIsActiveTrue(employeeId, businessId)).thenReturn(Optional.of(employee));
        // Apenas o serviço válido foi retornado pelo filtro de businessId
        when(serviceRepository.findAllByBusinessIdAndIdIn(businessId, List.of(validServiceId, foreignServiceId)))
                .thenReturn(List.of(validService));

        // Act & Assert
        assertThatThrownBy(() -> assignEmployeeServicesUseCase.execute(businessId, employeeId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Um ou mais serviços indicados não existem ou não pertencem ao salão");

        verify(employeeServiceRepository, never()).deleteAllByBusinessIdAndEmployeeId(any(), any());
    }
}
