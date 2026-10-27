package cv.perola.modules.business.service;

import cv.perola.core.security.TenantMembership;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.identity.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessUserMembershipProviderTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private BusinessUserMembershipProvider membershipProvider;

    @Test
    @DisplayName("Deve retornar lista vazia quando o userId for nulo")
    void shouldReturnEmptyListWhenUserIdIsNull() {
        // Arrange & Act
        List<TenantMembership> memberships = membershipProvider.findMembershipsByUserId(null);

        // Assert
        assertThat(memberships).isEmpty();
        verifyNoInteractions(businessRepository, employeeRepository);
    }

    @Test
    @DisplayName("Deve retornar membership OWNER quando o utilizador for proprietário")
    void shouldReturnOwnerMembershipWhenUserIsOwner() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        BusinessEntity business = BusinessEntity.builder()
                .ownerId(userId)
                .name("Salão do Zé")
                .build();
        business.setId(businessId);

        when(businessRepository.findByOwnerId(userId)).thenReturn(Optional.of(business));
        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        List<TenantMembership> memberships = membershipProvider.findMembershipsByUserId(userId);

        // Assert
        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).businessId()).isEqualTo(businessId);
        assertThat(memberships.get(0).role()).isEqualTo(UserRole.OWNER.name());
    }

    @Test
    @DisplayName("Deve retornar membership EMPLOYEE quando o utilizador for funcionário")
    void shouldReturnEmployeeMembershipWhenUserIsEmployee() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        EmployeeEntity employee = EmployeeEntity.builder()
                .businessId(businessId)
                .userId(userId)
                .displayName("Colaborador Maria")
                .build();

        when(businessRepository.findByOwnerId(userId)).thenReturn(Optional.empty());
        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

        // Act
        List<TenantMembership> memberships = membershipProvider.findMembershipsByUserId(userId);

        // Assert
        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).businessId()).isEqualTo(businessId);
        assertThat(memberships.get(0).role()).isEqualTo(UserRole.EMPLOYEE.name());
    }

    @Test
    @DisplayName("Não deve duplicar salão quando o utilizador for OWNER e EMPLOYEE do mesmo salão")
    void shouldNotDuplicateMembershipWhenUserIsOwnerAndEmployeeOfSameBusiness() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        BusinessEntity business = BusinessEntity.builder().ownerId(userId).build();
        business.setId(businessId);

        EmployeeEntity employee = EmployeeEntity.builder().businessId(businessId).userId(userId).build();

        when(businessRepository.findByOwnerId(userId)).thenReturn(Optional.of(business));
        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

        // Act
        List<TenantMembership> memberships = membershipProvider.findMembershipsByUserId(userId);

        // Assert
        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).businessId()).isEqualTo(businessId);
        assertThat(memberships.get(0).role()).isEqualTo(UserRole.OWNER.name());
    }

    @Test
    @DisplayName("Deve retornar ambas as memberships quando o utilizador for OWNER de um salão e EMPLOYEE de outro")
    void shouldReturnBothMembershipsWhenOwnerOfOneAndEmployeeOfAnother() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessAId = UUID.randomUUID();
        UUID businessBId = UUID.randomUUID();

        BusinessEntity businessA = BusinessEntity.builder().ownerId(userId).build();
        businessA.setId(businessAId);

        EmployeeEntity employeeB = EmployeeEntity.builder().businessId(businessBId).userId(userId).build();

        when(businessRepository.findByOwnerId(userId)).thenReturn(Optional.of(businessA));
        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employeeB));

        // Act
        List<TenantMembership> memberships = membershipProvider.findMembershipsByUserId(userId);

        // Assert
        assertThat(memberships).hasSize(2);
        assertThat(memberships).extracting(TenantMembership::businessId).containsExactly(businessAId, businessBId);
        assertThat(memberships).extracting(TenantMembership::role).containsExactly(UserRole.OWNER.name(), UserRole.EMPLOYEE.name());
    }
}
