package cv.perola.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import cv.perola.modules.identity.domain.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtro de isolamento Multi-Tenant responsável por:
 * 1. Validar e extrair o cabeçalho X-Business-Id em rotas protegidas de gestão.
 * 2. Garantir que o utilizador autenticado possui membership ativa (OWNER ou
 * EMPLOYEE) no salão.
 * 3. Impedir acessos cruzados (cross-tenant) entre salões distintos (HTTP 403
 * RFC 7807).
 * 4. Popular o TenantContext para a thread atual e limpá-lo obrigatoriamente no
 * bloco finally.
 */
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Business-Id";
    public static final String TENANT_ACCESS_DENIED_TYPE = "https://perola.cv/errors/tenant-access-denied";

    private static final Pattern BUSINESS_PATH_PATTERN = Pattern
            .compile("^/api/v1/businesses/([0-9a-fA-F\\-]{36})(?:/.*)?$");

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        String normalizedPath = normalizePath(path);
        String method = request.getMethod();

        // 1. Bypass CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Bypass rotas públicas que nunca requerem tenant
        if (isPublicRoute(normalizedPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Obter autenticação atual do Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            // Se não autenticado, permite avançar para que a cadeia do Spring Security gere
            // 401 Unauthorized
            filterChain.doFilter(request, response);
            return;
        }

        String tenantHeader = request.getHeader(TENANT_HEADER);
        boolean isTenantRequired = isTenantManagementRoute(normalizedPath, method);

        // 4. Se o cabeçalho estiver ausente
        if (tenantHeader == null || tenantHeader.isBlank()) {
            if (isTenantRequired) {
                writeProblemDetailsResponse(
                        response,
                        request.getRequestURI(),
                        "O cabeçalho X-Business-Id é obrigatório para aceder a este recurso de gestão.");
                return;
            }
            // Rota autenticada não requer tenant (ex: /api/v1/users/me, onboarding,
            // reservas do cliente)
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Se o cabeçalho foi fornecido, validar sintaxe UUID
        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantHeader.trim());
        } catch (IllegalArgumentException ex) {
            writeProblemDetailsResponse(
                    response,
                    request.getRequestURI(),
                    "O cabeçalho X-Business-Id fornecido não é um UUID válido.");
            return;
        }

        // 6. Validar pertença (membership) ativa do utilizador ao negócio
        boolean hasActiveMembership = principal.getMemberships() != null && principal.getMemberships().stream()
                .anyMatch(m -> tenantId.equals(m.businessId()) &&
                        (UserRole.OWNER.name().equalsIgnoreCase(m.role()) ||
                                UserRole.EMPLOYEE.name().equalsIgnoreCase(m.role())));

        if (!hasActiveMembership) {
            writeProblemDetailsResponse(
                    response,
                    request.getRequestURI(),
                    "O utilizador não possui permissões de gestão para o salão especificado.");
            return;
        }

        // 7. Validar coerência entre ID da rota (se presente) e o cabeçalho
        // X-Business-Id
        UUID pathBusinessId = extractBusinessIdFromPath(normalizedPath);
        if (pathBusinessId != null && !pathBusinessId.equals(tenantId)) {
            writeProblemDetailsResponse(
                    response,
                    request.getRequestURI(),
                    "O cabeçalho X-Business-Id não coincide com o identificador do salão indicado na rota.");
            return;
        }

        // 8. Executar requisição com TenantContext ativo e limpeza garantida no finally
        try {
            TenantContext.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicRoute(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/marketplace")
                || path.startsWith("/api/v1/availability")
                || path.startsWith("/api/v1/schedule/holidays");
    }

    private boolean isTenantManagementRoute(String normalizedPath, String method) {
        // Onboarding de salão (POST /api/v1/businesses) não exige tenant prévio
        if ("POST".equalsIgnoreCase(method) && normalizedPath.equals("/api/v1/businesses")) {
            return false;
        }
        if (normalizedPath.startsWith("/api/v1/businesses/") || normalizedPath.equals("/api/v1/businesses")) {
            return true;
        }

        // Gestão de horários do profissional (/api/v1/schedule/** exceto /holidays)
        if (normalizedPath.startsWith("/api/v1/schedule") && !normalizedPath.startsWith("/api/v1/schedule/holidays")) {
            return true;
        }

        // Agenda de gestão do salão (GET /api/v1/appointments)
        if ("GET".equalsIgnoreCase(method) && normalizedPath.equals("/api/v1/appointments")) {
            return true;
        }

        // Ações de gestão de agendamentos pelo profissional/salão
        if (normalizedPath.matches("^/api/v1/appointments/[^/]+/confirm$")) {
            return true;
        }
        if (normalizedPath.matches("^/api/v1/appointments/[^/]+/complete$")) {
            return true;
        }

        return false;
    }

    private UUID extractBusinessIdFromPath(String path) {
        Matcher matcher = BUSINESS_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            try {
                return UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return (path.length() > 1 && path.endsWith("/")) ? path.substring(0, path.length() - 1) : path;
    }

    private void writeProblemDetailsResponse(HttpServletResponse response, String instance, String detail)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", TENANT_ACCESS_DENIED_TYPE);
        problem.put("title", "Acesso ao Salão Negado");
        problem.put("status", HttpServletResponse.SC_FORBIDDEN);
        problem.put("detail", detail);
        problem.put("instance", instance);
        problem.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
