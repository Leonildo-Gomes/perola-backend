package cv.perola.core.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    private static final UUID SALON_A_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SALON_B_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve definir e obter o tenant ID corretamente na thread atual")
    void shouldSetAndGetTenantId() {
        // Arrange
        UUID expectedTenantId = SALON_A_ID;

        // Act
        TenantContext.setTenantId(expectedTenantId);

        // Assert
        assertThat(TenantContext.getTenantId()).isEqualTo(expectedTenantId);
        assertThat(TenantContext.isSet()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar Optional contendo o tenant ID quando estiver definido")
    void shouldFindTenantIdWhenPresent() {
        // Arrange
        TenantContext.setTenantId(SALON_A_ID);

        // Act
        Optional<UUID> tenantIdOpt = TenantContext.findTenantId();

        // Assert
        assertThat(tenantIdOpt).isPresent().contains(SALON_A_ID);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando o tenant ID não estiver definido")
    void shouldReturnEmptyOptionalWhenTenantIdNotPresent() {
        // Arrange & Act
        Optional<UUID> tenantIdOpt = TenantContext.findTenantId();

        // Assert
        assertThat(tenantIdOpt).isEmpty();
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar o tenant ID com requireTenantId quando estiver definido")
    void shouldReturnTenantIdWithRequireTenantId() {
        // Arrange
        TenantContext.setTenantId(SALON_A_ID);

        // Act
        UUID tenantId = TenantContext.requireTenantId();

        // Assert
        assertThat(tenantId).isEqualTo(SALON_A_ID);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException em requireTenantId quando nenhum tenant estiver definido")
    void shouldThrowIllegalStateExceptionWhenRequireTenantIdWithoutContext() {
        // Arrange & Act & Assert
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nenhum contexto de tenant ativo encontrado na thread atual.");
    }

    @Test
    @DisplayName("Deve limpar o tenant ID ao chamar clear")
    void shouldClearTenantId() {
        // Arrange
        TenantContext.setTenantId(SALON_A_ID);
        assertThat(TenantContext.getTenantId()).isNotNull();

        // Act
        TenantContext.clear();

        // Assert
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(TenantContext.isSet()).isFalse();
        assertThat(TenantContext.findTenantId()).isEmpty();
    }

    @Test
    @DisplayName("Deve manter isolamento de tenant entre threads distintas")
    void shouldIsolateTenantContextBetweenThreads() throws InterruptedException {
        // Arrange
        TenantContext.setTenantId(SALON_A_ID);
        AtomicReference<UUID> threadTenantRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Act
        Thread workerThread = new Thread(() -> {
            TenantContext.setTenantId(SALON_B_ID);
            threadTenantRef.set(TenantContext.getTenantId());
            TenantContext.clear();
            latch.countDown();
        });
        workerThread.start();
        latch.await();

        // Assert
        assertThat(threadTenantRef.get()).isEqualTo(SALON_B_ID);
        assertThat(TenantContext.getTenantId()).isEqualTo(SALON_A_ID);
    }
}
