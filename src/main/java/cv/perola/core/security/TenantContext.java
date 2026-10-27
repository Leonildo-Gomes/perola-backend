package cv.perola.core.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Armazena e gere o identificador de negócio (Tenant ID) associado à thread da requisição atual.
 * Utiliza {@link ThreadLocal} para garantir isolamento seguro entre requisições concorrentes.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Construtor privado para impedir instanciação da classe utilitária
    }

    /**
     * Define o Tenant ID para a thread atual.
     *
     * @param tenantId Identificador único do negócio ativo
     */
    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Obtém o Tenant ID associado à thread atual.
     *
     * @return O UUID do negócio ou {@code null} se não estiver definido
     */
    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Obtém o Tenant ID da thread atual encapsulado num {@link Optional}.
     *
     * @return {@link Optional} contendo o UUID do negócio, ou vazio se não estiver definido
     */
    public static Optional<UUID> findTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    /**
     * Obtém obrigatoriamente o Tenant ID da thread atual.
     *
     * @return O UUID do negócio ativo
     * @throws IllegalStateException se nenhum contexto de tenant estiver ativo na thread
     */
    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Nenhum contexto de tenant ativo encontrado na thread atual.");
        }
        return tenantId;
    }

    /**
     * Verifica se existe um Tenant ID ativo na thread atual.
     *
     * @return {@code true} se um tenant estiver definido, {@code false} caso contrário
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Limpa o Tenant ID da thread atual para prevenir vazamento de estado em pools de threads.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
