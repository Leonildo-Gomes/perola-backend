package cv.perola.modules.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.security.JwtService;
import cv.perola.core.security.TenantMembership;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BusinessIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private UserEntity createTestUser(String email, String name) {
        UserEntity user = UserEntity.builder()
                .name(name)
                .email(email)
                .phone("+23899" + (int) (Math.random() * 90000 + 10000))
                .passwordHash("hashed_password")
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    private String generateToken(UserEntity user, List<TenantMembership> memberships) {
        return jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                List.of("CUSTOMER"),
                memberships != null ? memberships : Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Deve criar salão, atribuir papel OWNER e criar funcionário inicial com sucesso")
    void shouldCreateBusinessAndInitialEmployeeAndAssignOwnerRoleWhenRequestIsValid() throws Exception {
        // Arrange
        UserEntity user = createTestUser("maria.dona@perola.cv", "Dona Maria");
        String token = generateToken(user, Collections.emptyList());

        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Dona Maria",
                "Santiago",
                "Praia",
                "Terra Branca, Rua Principal nº 10",
                "+2382619988",
                "Atlantic/Cape_Verde"
        );

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ownerId").value(user.getId().toString()))
                .andExpect(jsonPath("$.name").value("Salão Dona Maria"))
                .andExpect(jsonPath("$.island").value("Santiago"))
                .andExpect(jsonPath("$.city").value("Praia"))
                .andExpect(jsonPath("$.location").value("Terra Branca, Rua Principal nº 10"))
                .andExpect(jsonPath("$.contactPhone").value("+2382619988"))
                .andExpect(jsonPath("$.timezone").value("Atlantic/Cape_Verde"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        // Assert no banco de dados
        String responseBody = result.getResponse().getContentAsString();
        UUID businessId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

        Optional<BusinessEntity> savedBusiness = businessRepository.findById(businessId);
        assertThat(savedBusiness).isPresent();
        assertThat(savedBusiness.get().getOwnerId()).isEqualTo(user.getId());
        assertThat(savedBusiness.get().getName()).isEqualTo("Salão Dona Maria");

        Optional<EmployeeEntity> initialEmployee = employeeRepository.findByUserId(user.getId());
        assertThat(initialEmployee).isPresent();
        assertThat(initialEmployee.get().getBusinessId()).isEqualTo(businessId);
        assertThat(initialEmployee.get().getDisplayName()).isEqualTo("Dona Maria");
        assertThat(initialEmployee.get().isActive()).isTrue();

        assertThat(userRoleRepository.existsByUserIdAndRole(user.getId(), UserRole.OWNER)).isTrue();
        assertThat(userRoleRepository.existsByUserIdAndRole(user.getId(), UserRole.EMPLOYEE)).isTrue();
    }

    @Test
    @DisplayName("Deve retornar HTTP 409 Conflict quando o mesmo proprietário tentar criar um segundo negócio")
    void shouldReturn409ConflictWhenOwnerAttemptsToCreateSecondBusiness() throws Exception {
        // Arrange
        UserEntity user = createTestUser("joao.prop@perola.cv", "João Proprietário");
        String token = generateToken(user, Collections.emptyList());

        CreateBusinessRequest firstRequest = new CreateBusinessRequest(
                "Primeiro Salão",
                "Santiago",
                "Praia",
                "Platô",
                "+2382611111",
                "Atlantic/Cape_Verde"
        );

        mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        CreateBusinessRequest secondRequest = new CreateBusinessRequest(
                "Segundo Salão",
                "São Vicente",
                "Mindelo",
                "Rua de Lisboa",
                "+2382622222",
                "Atlantic/Cape_Verde"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/conflict"))
                .andExpect(jsonPath("$.title").value("Conflito de Dados"))
                .andExpect(jsonPath("$.detail").value("O utilizador já possui um salão registado. No MVP é permitido apenas 1 negócio por proprietário."));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 Unauthorized ao tentar criar salão sem token JWT")
    void shouldReturn401UnauthorizedWhenCreatingBusinessWithoutToken() throws Exception {
        // Arrange
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Fantasma",
                "Santiago",
                "Praia",
                "Palmarejo",
                "+2382613333",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/unauthorized"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 Bad Request quando payload de criação tiver dados inválidos")
    void shouldReturn400BadRequestWhenCreatingBusinessWithInvalidPayload() throws Exception {
        // Arrange
        UserEntity user = createTestUser("invalido@perola.cv", "Utilizador Teste");
        String token = generateToken(user, Collections.emptyList());

        CreateBusinessRequest request = new CreateBusinessRequest(
                "", // Nome vazio
                "", // Ilha vazia
                "", // Cidade vazia
                "", // Localização vazia
                "", // Telefone vazio
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams.length()").value(5));
    }

    @Test
    @DisplayName("Deve obter salão por ID com sucesso quando autenticado e com cabeçalho X-Business-Id correto")
    void shouldGetBusinessByIdWhenAuthorizedWithTenantHeader() throws Exception {
        // Arrange
        UserEntity user = createTestUser("ana.owner@perola.cv", "Ana Owner");

        BusinessEntity business = BusinessEntity.builder()
                .ownerId(user.getId())
                .name("Salão Elegance")
                .island("Sal")
                .city("Santa Maria")
                .location("Rua 1 de Junho")
                .contactPhone("+2382421111")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        business = businessRepository.save(business);

        String token = generateToken(user, List.of(new TenantMembership(business.getId(), "OWNER")));

        // Act & Assert
        mockMvc.perform(get("/api/v1/businesses/" + business.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(business.getId().toString()))
                .andExpect(jsonPath("$.name").value("Salão Elegance"))
                .andExpect(jsonPath("$.city").value("Santa Maria"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 403 Forbidden ao consultar salão com X-Business-Id de outro salão")
    void shouldReturn403ForbiddenWhenAccessingBusinessWithDifferentTenantHeader() throws Exception {
        // Arrange
        UserEntity user = createTestUser("lucas.owner@perola.cv", "Lucas Owner");
        UUID otherSalonId = UUID.randomUUID();

        BusinessEntity business = BusinessEntity.builder()
                .ownerId(user.getId())
                .name("Barbearia Moderna")
                .island("Santiago")
                .city("Praia")
                .location("Achada Grande")
                .contactPhone("+2382617777")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        business = businessRepository.save(business);

        // Token só dá acesso ao seu próprio salão
        String token = generateToken(user, List.of(new TenantMembership(business.getId(), "OWNER")));

        // Act & Assert: header diverge do path
        mockMvc.perform(get("/api/v1/businesses/" + business.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", otherSalonId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/tenant-access-denied"));
    }

    @Test
    @DisplayName("Deve atualizar configurações do salão com sucesso via PATCH quando autorizado")
    void shouldUpdateBusinessWhenAuthorized() throws Exception {
        // Arrange
        UserEntity user = createTestUser("patricia.owner@perola.cv", "Patrícia Owner");

        BusinessEntity business = BusinessEntity.builder()
                .ownerId(user.getId())
                .name("Salão Inicial")
                .island("Santiago")
                .city("Praia")
                .location("Palmarejo")
                .contactPhone("+2382618888")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        business = businessRepository.save(business);

        String token = generateToken(user, List.of(new TenantMembership(business.getId(), "OWNER")));

        UpdateBusinessRequest updateRequest = new UpdateBusinessRequest(
                "Salão Renovado VIP",
                null,
                null,
                "Palmarejo Grande, Lote 14",
                "+2382619999",
                null
        );

        // Act & Assert
        mockMvc.perform(patch("/api/v1/businesses/" + business.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(business.getId().toString()))
                .andExpect(jsonPath("$.name").value("Salão Renovado VIP"))
                .andExpect(jsonPath("$.location").value("Palmarejo Grande, Lote 14"))
                .andExpect(jsonPath("$.contactPhone").value("+2382619999"))
                .andExpect(jsonPath("$.city").value("Praia")); // Preservado

        // Assert no banco
        BusinessEntity updatedInDb = businessRepository.findById(business.getId()).orElseThrow();
        assertThat(updatedInDb.getName()).isEqualTo("Salão Renovado VIP");
        assertThat(updatedInDb.getLocation()).isEqualTo("Palmarejo Grande, Lote 14");
        assertThat(updatedInDb.getContactPhone()).isEqualTo("+2382619999");
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 Not Found quando salão consultado não existir")
    void shouldReturn404NotFoundWhenBusinessDoesNotExist() throws Exception {
        // Arrange
        UserEntity user = createTestUser("notFound@perola.cv", "User NotFound");
        UUID nonExistentId = UUID.randomUUID();
        String token = generateToken(user, List.of(new TenantMembership(nonExistentId, "OWNER")));

        // Act & Assert
        mockMvc.perform(get("/api/v1/businesses/" + nonExistentId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", nonExistentId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/resource-not-found"));
    }
}
