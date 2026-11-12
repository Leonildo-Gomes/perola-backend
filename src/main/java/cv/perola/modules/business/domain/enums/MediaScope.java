package cv.perola.modules.business.domain.enums;

/**
 * Escopo funcional do ficheiro de imagem carregado via {@code POST /api/v1/media/upload}.
 */
public enum MediaScope {
    USER_AVATAR,
    BUSINESS_LOGO,
    SERVICE_IMAGE,
    EMPLOYEE_AVATAR;

    public boolean requiresBusinessTenant() {
        return this != USER_AVATAR;
    }
}

