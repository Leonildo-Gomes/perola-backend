package cv.perola.core.exception;

/**
 * Exceção lançada quando uma operação viola o isolamento de dados multi-tenant
 * ou quando o utilizador não possui acesso ao salão especificado.
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
