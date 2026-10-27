package cv.perola.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.config.JacksonConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SALON_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SALON_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ByteArrayOutputStream responseBuffer;
    private ObjectMapper objectMapper;
    private TenantFilter tenantFilter;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new JacksonConfig().objectMapper();
        tenantFilter = new TenantFilter(objectMapper);
        responseBuffer = new ByteArrayOutputStream();

        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {}

            @Override
            public void write(int b) {
                responseBuffer.write(b);
            }
        };

        lenient().when(response.getOutputStream()).thenReturn(servletOutputStream);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateUser(List<TenantMembership> memberships) {
        UserPrincipal principal = new UserPrincipal(
                USER_ID,
                "maria@perola.cv",
                "hashedPass",
                List.of("CUSTOMER", "OWNER"),
                memberships,
                true
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Deve permitir requisições CORS OPTIONS sem validação de tenant")
    void shouldBypassCorsPreflight() throws ServletException, IOException {
        // Arrange
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getServletPath()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/marketplace/businesses",
            "/api/v1/marketplace/services",
            "/api/v1/availability/slots",
            "/api/v1/schedule/holidays",
            "/api/v1/auth/login",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui/index.html"
    })
    @DisplayName("Deve permitir rotas públicas sem exigir X-Business-Id")
    void shouldBypassPublicRoutes(String publicPath) throws ServletException, IOException {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn(publicPath);

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve permitir passagem sem erro quando utilizador não está autenticado para que Spring Security responda 401")
    void shouldBypassWhenUserNotAuthenticated() throws ServletException, IOException {
        // Arrange
        SecurityContextHolder.clearContext();
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve permitir rotas autenticadas que não exigem tenant sem o cabeçalho X-Business-Id")
    void shouldBypassNonTenantRouteWithoutHeader() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(null);

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve permitir endpoint de onboarding (POST /api/v1/businesses) sem o cabeçalho X-Business-Id")
    void shouldBypassBusinessOnboardingWithoutHeader() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of());
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/api/v1/businesses");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(null);

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando rota de gestão não contém o cabeçalho X-Business-Id")
    void shouldRejectTenantRequiredRouteWhenHeaderMissing() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getRequestURI()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(null);

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
        String responseJson = responseBuffer.toString();
        assertThat(responseJson)
                .contains(TenantFilter.TENANT_ACCESS_DENIED_TYPE)
                .contains("O cabeçalho X-Business-Id é obrigatório");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando o cabeçalho X-Business-Id não é um UUID válido")
    void shouldRejectWhenTenantHeaderIsNotAValidUuid() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getRequestURI()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn("uuid-invalido");

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
        String responseJson = responseBuffer.toString();
        assertThat(responseJson)
                .contains(TenantFilter.TENANT_ACCESS_DENIED_TYPE)
                .contains("não é um UUID válido");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando utilizador autenticado do Salão A envia X-Business-Id do Salão B")
    void shouldRejectWhenUserAuthenticatedOnSalonASendsSalonBTenantHeader() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/appointments");
        when(request.getRequestURI()).thenReturn("/api/v1/appointments");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(SALON_B_ID.toString());

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
        String responseJson = responseBuffer.toString();
        assertThat(responseJson)
                .contains(TenantFilter.TENANT_ACCESS_DENIED_TYPE)
                .contains("não possui permissões de gestão para o salão especificado");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando papel do utilizador no salão não é OWNER nem EMPLOYEE")
    void shouldRejectWhenUserRoleInTenantIsNotOwnerOrEmployee() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "CUSTOMER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/appointments");
        when(request.getRequestURI()).thenReturn("/api/v1/appointments");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(SALON_A_ID.toString());

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
        String responseJson = responseBuffer.toString();
        assertThat(responseJson)
                .contains(TenantFilter.TENANT_ACCESS_DENIED_TYPE)
                .contains("não possui permissões de gestão");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando identificador na rota não coincide com o cabeçalho X-Business-Id")
    void shouldRejectWhenPathBusinessIdDoesNotMatchTenantHeader() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER"), new TenantMembership(SALON_B_ID, "EMPLOYEE")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getRequestURI()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(SALON_B_ID.toString());

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
        String responseJson = responseBuffer.toString();
        assertThat(responseJson)
                .contains(TenantFilter.TENANT_ACCESS_DENIED_TYPE)
                .contains("não coincide com o identificador do salão indicado na rota");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve autorizar requisição válida, popular o TenantContext e limpá-lo após execução")
    void shouldAllowAndSetTenantContextWhenValid() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/businesses/" + SALON_A_ID + "/services");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(SALON_A_ID.toString());

        AtomicReference<UUID> capturedTenantInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedTenantInChain.set(TenantContext.getTenantId());
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        assertThat(capturedTenantInChain.get()).isEqualTo(SALON_A_ID);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve garantir limpeza do TenantContext no bloco finally mesmo se a cadeia lançar exceção")
    void shouldAlwaysClearTenantContextInFinallyBlockEvenIfFilterChainThrows() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "EMPLOYEE")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/appointments");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(SALON_A_ID.toString());

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo(SALON_A_ID);
            throw new RuntimeException("Erro simulado no controlador");
        }).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThatThrownBy(() -> tenantFilter.doFilter(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro simulado no controlador");

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve autorizar e popular TenantContext quando cabeçalho válido for enviado em rota geral autenticada")
    void shouldAllowValidTenantHeaderOnNonTenantRoute() throws ServletException, IOException {
        // Arrange
        authenticateUser(List.of(new TenantMembership(SALON_A_ID, "OWNER")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader(TenantFilter.TENANT_HEADER)).thenReturn(SALON_A_ID.toString());

        AtomicReference<UUID> capturedTenantInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedTenantInChain.set(TenantContext.getTenantId());
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(capturedTenantInChain.get()).isEqualTo(SALON_A_ID);
        assertThat(TenantContext.getTenantId()).isNull();
    }
}
