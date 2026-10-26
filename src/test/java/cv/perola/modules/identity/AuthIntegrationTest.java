package cv.perola.modules.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.security.JwtService;
import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.GoogleLoginRequest;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.dto.RefreshTokenRequest;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — Deve registar utilizador com sucesso e emitir JWT válido")
    void shouldRegisterUserSuccessfullyAndReturnTokens() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Ana Monteiro",
                "ana.monteiro@perola.cv",
                "+2389910011",
                "senhaForte123"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.name").value("Ana Monteiro"))
                .andExpect(jsonPath("$.user.email").value("ana.monteiro@perola.cv"))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        // Valida claims do Access Token (HS256)
        Claims claims = jwtService.extractAllClaims(authResponse.accessToken());
        assertThat(claims.getSubject()).isEqualTo(authResponse.user().id().toString());
        assertThat(claims.get("email")).isEqualTo("ana.monteiro@perola.cv");
        assertThat(claims.get("roles", List.class)).asList().contains("CUSTOMER");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — Deve falhar com 409 quando o email já existe")
    void shouldFailRegistrationWhenEmailAlreadyExists() throws Exception {
        RegisterRequest request1 = new RegisterRequest(
                "Utilizador Um",
                "repetido@perola.cv",
                "+2389910022",
                "senhaForte123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        RegisterRequest request2 = new RegisterRequest(
                "Utilizador Dois",
                "repetido@perola.cv",
                "+2389910033",
                "outraSenha123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/conflict"))
                .andExpect(jsonPath("$.title").value("Conflito de Dados"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — Deve falhar com 400 (RFC 7807) quando a validação de campos falha")
    void shouldFailRegistrationWhenValidationFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "", // nome vazio
                "email-invalido", // formato inválido
                "",
                "curto" // menos de 8 caracteres
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                .andExpect(jsonPath("$.title").value("Erro de Validação"))
                .andExpect(jsonPath("$.invalidParams").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — Deve autenticar com sucesso e emitir novo par de tokens")
    void shouldAuthenticateUserWithValidCredentials() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Nelson Lima",
                "nelson.lima@perola.cv",
                "+2389910044",
                "segredoSeguro2026"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("nelson.lima@perola.cv", "segredoSeguro2026");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("nelson.lima@perola.cv"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — Deve rejeitar com 401 RFC 7807 quando a senha está errada")
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Teresa Pires",
                "teresa.pires@perola.cv",
                "+2389910055",
                "senhaCorreta123"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest wrongLogin = new LoginRequest("teresa.pires@perola.cv", "senhaErrada999");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/invalid-credentials"))
                .andExpect(jsonPath("$.title").value("Credenciais Inválidas"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/auth/google — Deve autenticar ou criar utilizador via Google OIDC")
    void shouldAuthenticateWithGoogleOidcToken() throws Exception {
        String mockToken = "mock-google-" + UUID.randomUUID() + ":google.user@perola.cv:Google User:https://avatar.url/photo.jpg";
        GoogleLoginRequest request = new GoogleLoginRequest(mockToken);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("google.user@perola.cv"))
                .andExpect(jsonPath("$.user.avatarUrl").value("https://avatar.url/photo.jpg"));

        assertThat(userRepository.findByEmail("google.user@perola.cv")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh — Deve rotacionar o refresh token e invalidar o anterior")
    void shouldRotateRefreshTokenSuccessfully() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Manuel Gomes",
                "manuel.gomes@perola.cv",
                "+2389910066",
                "minhaSenhaRotacao"
        );
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse initialAuth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        String oldRefreshToken = initialAuth.refreshToken();

        // 1. Refresh bem-sucedido
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(oldRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse rotatedAuth = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(rotatedAuth.refreshToken()).isNotEqualTo(oldRefreshToken);

        // 2. Tentativa de reusar o token antigo deve falhar com 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/invalid-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout — Deve revogar o token e responder 204 No Content")
    void shouldRevokeTokenOnLogout() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Sónia Lopes",
                "sonia.lopes@perola.cv",
                "+2389910077",
                "senhaLogout123"
        );
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);

        // Logout
        RefreshTokenRequest logoutReq = new RefreshTokenRequest(auth.refreshToken());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isNoContent());

        // Refresh com token revogado deve falhar
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me — Deve aceder com sucesso utilizando token Bearer JWT válido")
    void shouldAccessProtectedEndpointWithValidJwt() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Duarte Neves",
                "duarte.neves@perola.cv",
                "+2389910088",
                "senhaPerfilSegura123"
        );
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Duarte Neves"))
                .andExpect(jsonPath("$.email").value("duarte.neves@perola.cv"))
                .andExpect(jsonPath("$.phone").value("+2389910088"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me — Deve falhar com 401 RFC 7807 quando sem token de autenticação")
    void shouldRejectAccessToProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/unauthorized"))
                .andExpect(jsonPath("$.title").value("Não Autorizado"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/users/me — Deve falhar com 401 quando o token é inválido ou forjado")
    void shouldRejectAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer token-invalido-forjado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/unauthorized"));
    }
}
