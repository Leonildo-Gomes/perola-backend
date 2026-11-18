package cv.perola.modules.booking;

import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.repository.CustomerRepository;
import cv.perola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BusinessCustomerPersistenceTest {

    @Autowired
    private BusinessCustomerRepository businessCustomerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID createTestBusiness(String name) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity owner = userRepository.saveAndFlush(UserEntity.builder()
                .name("Owner " + name)
                .email("owner-" + suffix + "@perola.cv")
                .phone("+23899" + suffix.substring(0, 5))
                .passwordHash("pwd")
                .isActive(true)
                .build());

        UUID businessId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO businesses (id, owner_id, name, island, city, location, contact_phone) VALUES (?, ?, ?, ?, ?, ?, ?)",
                businessId, owner.getId(), name, "Santiago", "Praia", "Plateau", "+2382611111"
        );
        return businessId;
    }

    private CustomerEntity createTestCustomer(String name, String email) {
        CustomerEntity customer = CustomerEntity.builder()
                .name(name)
                .email(email)
                .phone("+2389912345")
                .build();
        return customerRepository.saveAndFlush(customer);
    }

    @Test
    @DisplayName("Deve persistir ficha de cliente do salão com tier NEW e notas internas")
    void shouldPersistAndRetrieveBusinessCustomer() {
        // Arrange
        UUID businessId = createTestBusiness("Salão Teste 1");
        CustomerEntity customer = createTestCustomer("Márcia Gonçalves", "marcia-" + UUID.randomUUID() + "@perola.cv");

        BusinessCustomerEntity entity = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customer.getId())
                .tier(CustomerTier.NEW)
                .internalNotes("Primeira visita")
                .build();

        // Act
        BusinessCustomerEntity saved = businessCustomerRepository.saveAndFlush(entity);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<BusinessCustomerEntity> found = businessCustomerRepository.findByBusinessIdAndCustomerId(businessId, customer.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTier()).isEqualTo(CustomerTier.NEW);
        assertThat(found.get().getInternalNotes()).isEqualTo("Primeira visita");
    }

    @Test
    @DisplayName("Cenário 5: Deve garantir isolamento multi-tenant de classificação (VIP no Salão A e NEW no Salão B)")
    void shouldEnforceMultiTenantClassificationIsolationWithoutConflict() {
        // Arrange
        UUID salonA = createTestBusiness("Salão Beleza Crioula");
        UUID salonB = createTestBusiness("Barbearia Platô");
        CustomerEntity customer = createTestCustomer("Cliente Multifacetado", "multi-" + UUID.randomUUID() + "@cv.com");

        BusinessCustomerEntity profileSalonA = BusinessCustomerEntity.builder()
                .businessId(salonA)
                .customerId(customer.getId())
                .tier(CustomerTier.VIP)
                .internalNotes("Cliente VIP que adora chá verde")
                .build();

        BusinessCustomerEntity profileSalonB = BusinessCustomerEntity.builder()
                .businessId(salonB)
                .customerId(customer.getId())
                .tier(CustomerTier.NEW)
                .internalNotes("Primeira vez neste espaço")
                .build();

        // Act
        businessCustomerRepository.saveAndFlush(profileSalonA);
        businessCustomerRepository.saveAndFlush(profileSalonB);

        // Assert
        Optional<BusinessCustomerEntity> inSalonA = businessCustomerRepository.findByBusinessIdAndCustomerId(salonA, customer.getId());
        Optional<BusinessCustomerEntity> inSalonB = businessCustomerRepository.findByBusinessIdAndCustomerId(salonB, customer.getId());

        assertThat(inSalonA).isPresent();
        assertThat(inSalonA.get().getTier()).isEqualTo(CustomerTier.VIP);
        assertThat(inSalonA.get().getInternalNotes()).contains("chá verde");

        assertThat(inSalonB).isPresent();
        assertThat(inSalonB.get().getTier()).isEqualTo(CustomerTier.NEW);
        assertThat(inSalonB.get().getInternalNotes()).contains("Primeira vez");
    }

    @Test
    @DisplayName("Deve impedir duplicação de vínculo para o mesmo (business_id, customer_id)")
    void shouldEnforceUniqueTenantCustomerConstraint() {
        // Arrange
        UUID businessId = createTestBusiness("Salão Exclusivo");
        CustomerEntity customer = createTestCustomer("Cliente Único", "unico-" + UUID.randomUUID() + "@cv.com");

        BusinessCustomerEntity first = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customer.getId())
                .tier(CustomerTier.REGULAR)
                .build();
        businessCustomerRepository.saveAndFlush(first);

        BusinessCustomerEntity duplicate = BusinessCustomerEntity.builder()
                .businessId(businessId)
                .customerId(customer.getId())
                .tier(CustomerTier.VIP)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> businessCustomerRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
