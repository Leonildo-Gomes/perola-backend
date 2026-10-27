package cv.perola.core.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.addSupportedVersions("v1")
                .setDefaultVersion("v1")
                .setVersionRequired(false)
                .useVersionResolver(new ApiPathVersionResolver());
    }

    /**
     * Extrai a versão da API apenas para rotas que iniciam com /api/.
     * Evita que rotas como /swagger-ui/index.html, /v3/api-docs e /actuator/**
     * tenham seus segmentos interpretados indevidamente como versão da API.
     */
    private static class ApiPathVersionResolver implements ApiVersionResolver {
        @Override
        public String resolveVersion(HttpServletRequest request) {
            String path = request.getServletPath();
            if (path == null || !path.startsWith("/api/")) {
                return null;
            }
            String[] segments = path.split("/");
            // Exemplo: /api/v1/auth/login -> segments: ["", "api", "v1", "auth", "login"]
            if (segments.length > 2 && !segments[2].isBlank()) {
                return segments[2];
            }
            return null;
        }
    }
}

