package cv.perola.modules.identity.service;

import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.repository.CustomerRepository;
import cv.perola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerResolutionServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultCustomerResolutionService customerResolutionService;

    @Test
    @DisplayName("Cenário 1: Deve criar novo registo de Customer com user_id=null quando visitante agenda pela primeira vez")
    void shouldCreateNewCustomerWithNullUserIdWhenGuestBooksForFirstTime() {
        // Arrange
        String email = "ana@cv.com";
        String name = "Ana Semedo";
        String phone = "+2389911111";

        when(customerRepository.findByEmailIgnoreCase("ana@cv.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
            CustomerEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        CustomerEntity result = customerResolutionService.resolveForBooking(name, email, phone, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNull();
        assertThat(result.getEmail()).isEqualTo("ana@cv.com");
        assertThat(result.getName()).isEqualTo("Ana Semedo");
        assertThat(result.getPhone()).isEqualTo("+2389911111");

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@cv.com");
    }

    @Test
    @DisplayName("Cenário 2: Deve reutilizar customer.id existente sem duplicação quando visitante recorrente agenda com mesmo email")
    void shouldReuseExistingCustomerIdWhenGuestBooksAgain() {
        // Arrange
        UUID existingCustomerId = UUID.randomUUID();
        CustomerEntity existing = CustomerEntity.builder()
                .userId(null)
                .name("Ana Antiga")
                .email("ana@cv.com")
                .phone("+2389911111")
                .build();
        existing.setId(existingCustomerId);

        when(customerRepository.findByEmailIgnoreCase("ana@cv.com")).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerEntity result = customerResolutionService.resolveForBooking("Ana Semedo Atualizada", "ANA@CV.COM", "+2389922222", null);

        // Assert
        assertThat(result.getId()).isEqualTo(existingCustomerId);
        assertThat(result.getUserId()).isNull();
        assertThat(result.getName()).isEqualTo("Ana Semedo Atualizada");
        assertThat(result.getPhone()).isEqualTo("+2389922222");
        verify(customerRepository).save(existing);
    }

    @Test
    @DisplayName("Cenário 3 (Anti-Spoofing): Não deve sobrescrever nome nem telefone de utilizador registado em agendamento anónimo")
    void shouldPreventSpoofingWhenGuestProvidesEmailOfRegisteredUser() {
        // Arrange
        UUID registeredUserId = UUID.randomUUID();
        UUID existingCustomerId = UUID.randomUUID();

        CustomerEntity registeredCustomer = CustomerEntity.builder()
                .userId(registeredUserId)
                .name("Márcia Gonçalves")
                .email("marcia@exemplo.cv")
                .phone("+2389912345")
                .build();
        registeredCustomer.setId(existingCustomerId);

        when(customerRepository.findByEmailIgnoreCase("marcia@exemplo.cv")).thenReturn(Optional.of(registeredCustomer));

        // Act
        CustomerEntity result = customerResolutionService.resolveForBooking(
                "Impostor ou Nome Diferente",
                "marcia@exemplo.cv",
                "+2389999999",
                null // Não autenticado
        );

        // Assert
        assertThat(result.getId()).isEqualTo(existingCustomerId);
        assertThat(result.getUserId()).isEqualTo(registeredUserId);
        assertThat(result.getName()).isEqualTo("Márcia Gonçalves");
        assertThat(result.getPhone()).isEqualTo("+2389912345");
        verify(customerRepository, never()).save(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("Deve permitir atualização de nome e telefone quando a chamada parte do próprio utilizador autenticado titular")
    void shouldAllowUpdatingNameAndPhoneWhenAuthenticatedUserIsAccountOwner() {
        // Arrange
        UUID registeredUserId = UUID.randomUUID();
        UUID existingCustomerId = UUID.randomUUID();

        CustomerEntity registeredCustomer = CustomerEntity.builder()
                .userId(registeredUserId)
                .name("Márcia Antiga")
                .email("marcia@exemplo.cv")
                .phone("+2389912345")
                .build();
        registeredCustomer.setId(existingCustomerId);

        when(customerRepository.findByEmailIgnoreCase("marcia@exemplo.cv")).thenReturn(Optional.of(registeredCustomer));
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerEntity result = customerResolutionService.resolveForBooking(
                "Márcia Gonçalves Atualizada",
                "marcia@exemplo.cv",
                "+2389988888",
                registeredUserId // Autenticado com o próprio ID do titular
        );

        // Assert
        assertThat(result.getId()).isEqualTo(existingCustomerId);
        assertThat(result.getUserId()).isEqualTo(registeredUserId);
        assertThat(result.getName()).isEqualTo("Márcia Gonçalves Atualizada");
        assertThat(result.getPhone()).isEqualTo("+2389988888");
        verify(customerRepository).save(registeredCustomer);
    }

    @Test
    @DisplayName("Cenário 4: Deve vincular customer.user_id ao id do novo utilizador no registo de conta")
    void shouldLinkUserIdToCustomerWhenGuestCreatesAccountLater() {
        // Arrange
        UUID newUserId = UUID.randomUUID();
        UUID guestCustomerId = UUID.randomUUID();

        CustomerEntity guestCustomer = CustomerEntity.builder()
                .userId(null)
                .name("Ana Semedo")
                .email("ana@cv.com")
                .phone("+2389911111")
                .build();
        guestCustomer.setId(guestCustomerId);

        when(customerRepository.findByEmailIgnoreCase("ana@cv.com")).thenReturn(Optional.of(guestCustomer));
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerEntity linkedCustomer = customerResolutionService.linkOrRegisterUser(
                newUserId,
                "Ana Semedo Silva",
                "ana@cv.com",
                "+2389922222"
        );

        // Assert
        assertThat(linkedCustomer.getId()).isEqualTo(guestCustomerId);
        assertThat(linkedCustomer.getUserId()).isEqualTo(newUserId);
        assertThat(linkedCustomer.getName()).isEqualTo("Ana Semedo Silva");
        assertThat(linkedCustomer.getPhone()).isEqualTo("+2389922222");
        verify(customerRepository).save(guestCustomer);
    }

    @Test
    @DisplayName("Deve criar novo Customer com user_id quando utilizador sem histórico prévio de visitante se regista")
    void shouldCreateNewCustomerWithUserIdWhenBrandNewUserRegisters() {
        // Arrange
        UUID newUserId = UUID.randomUUID();

        when(customerRepository.findByEmailIgnoreCase("novouser@perola.cv")).thenReturn(Optional.empty());
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
            CustomerEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        CustomerEntity result = customerResolutionService.linkOrRegisterUser(
                newUserId,
                "Novo Utilizador",
                "novouser@perola.cv",
                "+2389933333"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(newUserId);
        assertThat(result.getEmail()).isEqualTo("novouser@perola.cv");
        assertThat(result.getName()).isEqualTo("Novo Utilizador");
        assertThat(result.getPhone()).isEqualTo("+2389933333");
    }

    @Test
    @DisplayName("Deve resolver cliente para utilizador autenticado a partir do user_id")
    void shouldResolveForAuthenticatedUserSuccessfully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        CustomerEntity existingCustomer = CustomerEntity.builder()
                .userId(userId)
                .name("Cliente Autenticado")
                .email("auth@perola.cv")
                .phone("+2389911223")
                .build();

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(existingCustomer));

        // Act
        CustomerEntity result = customerResolutionService.resolveForAuthenticatedUser(userId);

        // Assert
        assertThat(result).isSameAs(existingCustomer);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve buscar e mapear resumos de clientes em lote para comunicação inter-módulos")
    void shouldFindCustomerSummariesInBatch() {
        // Arrange
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();
        CustomerEntity c1 = CustomerEntity.builder()
                .userId(UUID.randomUUID())
                .name("Cliente Um")
                .email("c1@perola.cv")
                .phone("+2389911111")
                .build();
        c1.setId(c1Id);

        CustomerEntity c2 = CustomerEntity.builder()
                .userId(null)
                .name("Cliente Dois")
                .email("c2@perola.cv")
                .phone("+2389922222")
                .build();
        c2.setId(c2Id);

        when(customerRepository.findAllById(List.of(c1Id, c2Id))).thenReturn(List.of(c1, c2));

        // Act
        Map<UUID, CustomerSummary> summaries = customerResolutionService.findCustomerSummariesByIds(List.of(c1Id, c2Id));

        // Assert
        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(c1Id).name()).isEqualTo("Cliente Um");
        assertThat(summaries.get(c1Id).userId()).isNotNull();
        assertThat(summaries.get(c2Id).name()).isEqualTo("Cliente Dois");
        assertThat(summaries.get(c2Id).userId()).isNull();
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o email for nulo ou em branco e não houver utilizador autenticado")
    void shouldThrowIllegalArgumentExceptionWhenEmailIsMissingAndNoAuthenticatedUser() {
        // Act & Assert
        assertThatThrownBy(() -> customerResolutionService.resolveForBooking("Sem Email", "  ", "+2389911111", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O email do cliente é obrigatório");
    }
}
