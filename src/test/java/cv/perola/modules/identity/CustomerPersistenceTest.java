package cv.perola.modules.identity;

import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.repository.CustomerRepository;
import cv.perola.modules.identity.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CustomerPersistenceTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deve persistir e recuperar cliente visitante (sem userId) com sucesso")
    void shouldCreateAndRetrieveGuestCustomer() {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        CustomerEntity customer = CustomerEntity.builder()
                .userId(null)
                .name("Visitante Sem Conta")
                .email("visitante-" + suffix + "@cv.com")
                .phone("+2389912345")
                .build();

        // Act
        CustomerEntity saved = customerRepository.saveAndFlush(customer);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<CustomerEntity> found = customerRepository.findByEmailIgnoreCase("VISITANTE-" + suffix + "@CV.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Visitante Sem Conta");
    }

    @Test
    @DisplayName("Deve persistir cliente vinculado a utilizador e aplicar ON DELETE SET NULL")
    void shouldCreateCustomerLinkedToUserAndSetNullOnUserDeletion() {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = UserEntity.builder()
                .name("Titular Conta")
                .email("titular-" + suffix + "@perola.cv")
                .phone("+2389923456")
                .passwordHash("pwd")
                .isActive(true)
                .build();
        UserEntity savedUser = userRepository.saveAndFlush(user);

        CustomerEntity customer = CustomerEntity.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .build();
        CustomerEntity savedCustomer = customerRepository.saveAndFlush(customer);

        // Act & Assert - Verificação inicial
        assertThat(savedCustomer.getUserId()).isEqualTo(savedUser.getId());
        Optional<CustomerEntity> byUser = customerRepository.findByUserId(savedUser.getId());
        assertThat(byUser).isPresent();

        // Eliminar o utilizador deve manter o customer vivo com userId=null
        userRepository.delete(savedUser);
        userRepository.flush();
        entityManager.clear();

        Optional<CustomerEntity> afterDelete = customerRepository.findById(savedCustomer.getId());
        assertThat(afterDelete).isPresent();
        assertThat(afterDelete.get().getUserId()).isNull();
    }

    @Test
    @DisplayName("Deve impedir emails duplicados pela restrição uk_customers_email")
    void shouldEnforceUniqueEmailConstraint() {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "duplicado-" + suffix + "@cv.com";

        CustomerEntity c1 = CustomerEntity.builder()
                .userId(null)
                .name("Cliente 1")
                .email(email)
                .phone("+2389911111")
                .build();
        customerRepository.saveAndFlush(c1);

        CustomerEntity c2 = CustomerEntity.builder()
                .userId(null)
                .name("Cliente 2")
                .email(email)
                .phone("+2389922222")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> customerRepository.saveAndFlush(c2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
