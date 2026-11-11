package cv.perola.modules.business.service;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.identity.service.UserAccountService;
import cv.perola.modules.identity.service.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEmployeeEnrichmentServiceTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private ServiceAssignmentLookupService serviceAssignmentLookupService;

    @Mock
    private UserAccountService userAccountService;

    private final EmployeeMapper employeeMapper = new EmployeeMapper();

    private BusinessEmployeeEnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        enrichmentService = new BusinessEmployeeEnrichmentService(
                businessRepository,
                serviceAssignmentLookupService,
                userAccountService,
                employeeMapper
        );
    }

    @Test
    @DisplayName("Deve retornar lista vazia sem consultar dependências quando lista de colaboradores for vazia")
    void shouldReturnEmptyListWithoutQueryingDependenciesWhenEmployeesEmpty() {
        // Arrange
        UUID businessId = UUID.randomUUID();

        // Act
        List<BusinessEmployeeResponse> result = enrichmentService.enrich(businessId, List.of());

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(businessRepository, serviceAssignmentLookupService, userAccountService);
    }

    @Test
    @DisplayName("Deve enriquecer colaboradores em lote com email, telefone, avatar, accessRole e assignedServiceIds")
    void shouldEnrichEmployeesInBatchWithUserSummaryAccessRoleAndAssignedServices() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID staffUserId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        EmployeeEntity ownerEmp = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(ownerUserId)
                .displayName("Rita Dona")
                .isActive(true)
                .build();
        ownerEmp.setId(UUID.randomUUID());
        ownerEmp.setCreatedAt(Instant.now());
        ownerEmp.setUpdatedAt(Instant.now());

        EmployeeEntity staffEmp = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(staffUserId)
                .displayName("Maria Silva")
                .specialty("Trancista")
                .isActive(true)
                .build();
        staffEmp.setId(UUID.randomUUID());
        staffEmp.setCreatedAt(Instant.now());
        staffEmp.setUpdatedAt(Instant.now());

        EmployeeEntity offlineEmp = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(null)
                .displayName("Carlos Offline")
                .isActive(false)
                .build();
        offlineEmp.setId(UUID.randomUUID());
        offlineEmp.setCreatedAt(Instant.now());
        offlineEmp.setUpdatedAt(Instant.now());

        when(businessRepository.findOwnerIdById(businessId)).thenReturn(Optional.of(ownerUserId));
        when(serviceAssignmentLookupService.findAssignedServiceIdsByEmployee(
                businessId,
                List.of(ownerEmp.getId(), staffEmp.getId(), offlineEmp.getId())
        )).thenReturn(Map.of(staffEmp.getId(), List.of(serviceId)));
        when(userAccountService.findUserSummariesByIds(Set.of(ownerUserId, staffUserId)))
                .thenReturn(Map.of(
                        ownerUserId, new UserSummary(ownerUserId, "rita@salao.cv", "+2389900001", null),
                        staffUserId, new UserSummary(staffUserId, "maria@salao.cv", "+2389912345", "https://cdn.perola.cv/maria.png")
                ));

        // Act
        List<BusinessEmployeeResponse> result = enrichmentService.enrich(
                businessId, List.of(ownerEmp, staffEmp, offlineEmp)
        );

        // Assert
        assertThat(result).hasSize(3);

        BusinessEmployeeResponse ownerResp = result.get(0);
        assertThat(ownerResp.accessRole()).isEqualTo("OWNER");
        assertThat(ownerResp.email()).isEqualTo("rita@salao.cv");
        assertThat(ownerResp.assignedServiceIds()).isEmpty();

        BusinessEmployeeResponse staffResp = result.get(1);
        assertThat(staffResp.accessRole()).isEqualTo("EMPLOYEE");
        assertThat(staffResp.email()).isEqualTo("maria@salao.cv");
        assertThat(staffResp.phone()).isEqualTo("+2389912345");
        assertThat(staffResp.avatarUrl()).isEqualTo("https://cdn.perola.cv/maria.png");
        assertThat(staffResp.specialty()).isEqualTo("Trancista");
        assertThat(staffResp.assignedServiceIds()).containsExactly(serviceId);

        BusinessEmployeeResponse offlineResp = result.get(2);
        assertThat(offlineResp.userId()).isNull();
        assertThat(offlineResp.accessRole()).isNull();
        assertThat(offlineResp.email()).isNull();
        assertThat(offlineResp.phone()).isNull();
        assertThat(offlineResp.avatarUrl()).isNull();
        assertThat(offlineResp.assignedServiceIds()).isEmpty();

        verify(userAccountService).findUserSummariesByIds(Set.of(ownerUserId, staffUserId));
    }

    @Test
    @DisplayName("Deve enriquecer um único colaborador via enrichSingle")
    void shouldEnrichSingleEmployee() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(userId)
                .displayName("Maria Silva")
                .isActive(true)
                .build();
        employee.setId(UUID.randomUUID());

        when(businessRepository.findOwnerIdById(businessId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(serviceAssignmentLookupService.findAssignedServiceIdsByEmployee(businessId, List.of(employee.getId())))
                .thenReturn(Map.of());
        when(userAccountService.findUserSummariesByIds(Set.of(userId)))
                .thenReturn(Map.of(userId, new UserSummary(userId, "maria@salao.cv", "+2389912345", null)));

        // Act
        BusinessEmployeeResponse result = enrichmentService.enrichSingle(businessId, employee);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("maria@salao.cv");
        assertThat(result.accessRole()).isEqualTo("EMPLOYEE");
    }
}

