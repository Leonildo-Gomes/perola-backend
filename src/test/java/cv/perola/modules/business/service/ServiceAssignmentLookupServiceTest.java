package cv.perola.modules.business.service;

import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository.ServiceEmployeeAssignmentProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAssignmentLookupServiceTest {

    @Mock
    private EmployeeServiceRepository employeeServiceRepository;

    @InjectMocks
    private ServiceAssignmentLookupService service;

    @Test
    @DisplayName("Deve agrupar os IDs de colaboradores ativos por serviceId em lote")
    void shouldGroupActiveEmployeeIdsByServiceId() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();

        ServiceEmployeeAssignmentProjection p1 = mock(ServiceEmployeeAssignmentProjection.class);
        when(p1.getServiceId()).thenReturn(s1);
        when(p1.getEmployeeId()).thenReturn(e1);

        ServiceEmployeeAssignmentProjection p2 = mock(ServiceEmployeeAssignmentProjection.class);
        when(p2.getServiceId()).thenReturn(s1);
        when(p2.getEmployeeId()).thenReturn(e2);

        when(employeeServiceRepository.findActiveAssignmentsByBusinessIdAndServiceIdIn(businessId, List.of(s1, s2)))
                .thenReturn(List.of(p1, p2));

        // Act
        Map<UUID, List<UUID>> result = service.findAssignedEmployeeIdsByService(businessId, List.of(s1, s2));

        // Assert
        assertThat(result).containsEntry(s1, List.of(e1, e2));
        assertThat(result).doesNotContainKey(s2);
        verify(employeeServiceRepository).findActiveAssignmentsByBusinessIdAndServiceIdIn(businessId, List.of(s1, s2));
    }

    @Test
    @DisplayName("Deve retornar mapa vazio sem consultar repositório quando lista de serviços for vazia")
    void shouldReturnEmptyMapWhenServiceIdsIsEmpty() {
        // Arrange
        UUID businessId = UUID.randomUUID();

        // Act
        Map<UUID, List<UUID>> result = service.findAssignedEmployeeIdsByService(businessId, List.of());

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(employeeServiceRepository);
    }

    @Test
    @DisplayName("Deve retornar lista de IDs para um único serviço")
    void shouldReturnAssignedEmployeeIdsForSingleService() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        ServiceEmployeeAssignmentProjection p = mock(ServiceEmployeeAssignmentProjection.class);
        when(p.getServiceId()).thenReturn(serviceId);
        when(p.getEmployeeId()).thenReturn(employeeId);

        when(employeeServiceRepository.findActiveAssignmentsByBusinessIdAndServiceIdIn(businessId, List.of(serviceId)))
                .thenReturn(List.of(p));

        // Act
        List<UUID> result = service.findAssignedEmployeeIds(businessId, serviceId);

        // Assert
        assertThat(result).containsExactly(employeeId);
    }

    @Test
    @DisplayName("Deve agrupar os IDs de serviços atribuídos por employeeId em lote")
    void shouldGroupAssignedServiceIdsByEmployeeId() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();

        ServiceEmployeeAssignmentProjection p1 = mock(ServiceEmployeeAssignmentProjection.class);
        when(p1.getEmployeeId()).thenReturn(e1);
        when(p1.getServiceId()).thenReturn(s1);

        ServiceEmployeeAssignmentProjection p2 = mock(ServiceEmployeeAssignmentProjection.class);
        when(p2.getEmployeeId()).thenReturn(e1);
        when(p2.getServiceId()).thenReturn(s2);

        when(employeeServiceRepository.findAssignmentsByBusinessIdAndEmployeeIdIn(businessId, List.of(e1, e2)))
                .thenReturn(List.of(p1, p2));

        // Act
        Map<UUID, List<UUID>> result = service.findAssignedServiceIdsByEmployee(businessId, List.of(e1, e2));

        // Assert
        assertThat(result).containsEntry(e1, List.of(s1, s2));
        assertThat(result).doesNotContainKey(e2);
        verify(employeeServiceRepository).findAssignmentsByBusinessIdAndEmployeeIdIn(businessId, List.of(e1, e2));
    }

    @Test
    @DisplayName("Deve retornar mapa vazio sem consultar repositório quando lista de colaboradores for vazia")
    void shouldReturnEmptyMapWhenEmployeeIdsIsEmpty() {
        // Arrange
        UUID businessId = UUID.randomUUID();

        // Act
        Map<UUID, List<UUID>> result = service.findAssignedServiceIdsByEmployee(businessId, List.of());

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(employeeServiceRepository);
    }
}

