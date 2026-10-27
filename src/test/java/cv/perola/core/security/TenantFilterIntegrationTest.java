package cv.perola.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.config.JacksonConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantFilterIntegrationTest {

    private static final UUID USER_A_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SALON_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SALON_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private MockMvc mockMvc;
    private JwtService jwtService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfig().objectMapper();
        jwtService = new JwtService(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                3600,
                604800
        );
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        TenantFilter tenantFilter = new TenantFilter(objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestTenantEndpointsController())
                .addFilters(jwtAuthenticationFilter, tenantFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private String generateToken(UUID userId, String email, List<String> roles, List<TenantMembership> memberships) {
        return jwtService.generateAccessToken(userId, email, roles, memberships);
    }

    @Test
    @DisplayName("Critério de Aceitação: Utilizador do Salão A recebe HTTP 403 se enviar X-Business-Id do Salão B")
    void shouldReturn403WhenUserFromSalonASendsSalonBTenantHeader() throws Exception {
        // Arrange
        String token = generateToken(
                USER_A_ID,
                "proprietario.a@perola.cv",
                List.of("CUSTOMER", "OWNER"),
                List.of(new TenantMembership(SALON_A_ID, "OWNER"))
        );

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", SALON_B_ID.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(TenantFilter.TENANT_ACCESS_DENIED_TYPE))
                .andExpect(jsonPath("$.title").value("Acesso ao Salão Negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("O utilizador não possui permissões de gestão para o salão especificado."))
                .andExpect(jsonPath("$.instance").value("/api/v1/appointments"));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Critério de Aceitação: Rotas públicas do marketplace não exigem X-Business-Id")
    void shouldAllowPublicMarketplaceRouteWithoutTenantHeader() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/v1/marketplace/businesses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("marketplace-open"));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve autorizar acesso e popular TenantContext quando token e X-Business-Id forem válidos")
    void shouldAllowAndPopulateTenantContextWhenCredentialsAndTenantHeaderMatch() throws Exception {
        // Arrange
        String token = generateToken(
                USER_A_ID,
                "proprietario.a@perola.cv",
                List.of("CUSTOMER", "OWNER"),
                List.of(new TenantMembership(SALON_A_ID, "OWNER"))
        );

        // Act & Assert
        mockMvc.perform(get("/api/v1/businesses/" + SALON_A_ID + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", SALON_A_ID.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.tenantId").value(SALON_A_ID.toString()))
                .andExpect(jsonPath("$.pathId").value(SALON_A_ID.toString()));

        // Garantir que no final da requisição o TenantContext foi limpo
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando rota de gestão for chamada sem o cabeçalho X-Business-Id")
    void shouldReturn403WhenManagementRouteCalledWithoutTenantHeader() throws Exception {
        // Arrange
        String token = generateToken(
                USER_A_ID,
                "proprietario.a@perola.cv",
                List.of("CUSTOMER", "OWNER"),
                List.of(new TenantMembership(SALON_A_ID, "OWNER"))
        );

        // Act & Assert
        mockMvc.perform(get("/api/v1/businesses/" + SALON_A_ID + "/services")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(TenantFilter.TENANT_ACCESS_DENIED_TYPE))
                .andExpect(jsonPath("$.title").value("Acesso ao Salão Negado"))
                .andExpect(jsonPath("$.detail").value("O cabeçalho X-Business-Id é obrigatório para aceder a este recurso de gestão."));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando X-Business-Id for formato inválido (não-UUID)")
    void shouldReturn403WhenTenantHeaderIsMalformed() throws Exception {
        // Arrange
        String token = generateToken(
                USER_A_ID,
                "proprietario.a@perola.cv",
                List.of("CUSTOMER", "OWNER"),
                List.of(new TenantMembership(SALON_A_ID, "OWNER"))
        );

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", "not-a-valid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(TenantFilter.TENANT_ACCESS_DENIED_TYPE))
                .andExpect(jsonPath("$.detail").value("O cabeçalho X-Business-Id fornecido não é um UUID válido."));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar com HTTP 403 quando X-Business-Id não coincide com o ID do salão no path da rota")
    void shouldReturn403WhenPathBusinessIdDoesNotMatchTenantHeader() throws Exception {
        // Arrange: Utilizador tem permissão em ambos os salões, mas tenta aceder a rota do Salão A enviando header do Salão B
        String token = generateToken(
                USER_A_ID,
                "gestor@perola.cv",
                List.of("CUSTOMER", "OWNER"),
                List.of(new TenantMembership(SALON_A_ID, "OWNER"), new TenantMembership(SALON_B_ID, "OWNER"))
        );

        // Act & Assert
        mockMvc.perform(get("/api/v1/businesses/" + SALON_A_ID + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", SALON_B_ID.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(TenantFilter.TENANT_ACCESS_DENIED_TYPE))
                .andExpect(jsonPath("$.detail").value("O cabeçalho X-Business-Id não coincide com o identificador do salão indicado na rota."));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Deve autorizar endpoint de perfil /api/v1/users/me sem exigir X-Business-Id")
    void shouldAllowUserProfileWithoutTenantHeader() throws Exception {
        // Arrange
        String token = generateToken(
                USER_A_ID,
                "cliente@perola.cv",
                List.of("CUSTOMER"),
                List.of()
        );

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("user-profile"));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @RestController
    static class TestTenantEndpointsController {

        @GetMapping("/api/v1/businesses/{id}/services")
        public Map<String, Object> getSalonServices(@PathVariable UUID id) {
            return Map.of(
                    "status", "success",
                    "tenantId", TenantContext.getTenantId().toString(),
                    "pathId", id.toString()
            );
        }

        @GetMapping("/api/v1/schedule/employees/{id}/monthly")
        public Map<String, Object> getEmployeeSchedule(@PathVariable UUID id) {
            return Map.of(
                    "status", "success",
                    "tenantId", TenantContext.getTenantId().toString()
            );
        }

        @GetMapping("/api/v1/appointments")
        public Map<String, Object> getSalonAppointments() {
            return Map.of(
                    "status", "success",
                    "tenantId", TenantContext.getTenantId().toString()
            );
        }

        @PostMapping("/api/v1/appointments/{id}/confirm")
        public Map<String, Object> confirmAppointment(@PathVariable UUID id) {
            return Map.of(
                    "status", "confirmed",
                    "tenantId", TenantContext.getTenantId().toString()
            );
        }

        @GetMapping("/api/v1/marketplace/businesses")
        public Map<String, Object> getMarketplaceBusinesses() {
            return Map.of("status", "marketplace-open");
        }

        @GetMapping("/api/v1/users/me")
        public Map<String, Object> getCurrentUser() {
            return Map.of("status", "user-profile");
        }
    }
}
