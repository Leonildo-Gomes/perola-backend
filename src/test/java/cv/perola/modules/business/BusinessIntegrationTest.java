package cv.perola.modules.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.security.JwtService;
import cv.perola.core.security.TenantMembership;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.AssignEmployeeServicesRequest;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.EmployeeServiceRepository;
import cv.perola.modules.business.repository.ServiceRepository;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private EmployeeServiceRepository employeeServiceRepository;

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

    @Test
    @DisplayName("BE-007: Deve criar, listar, atualizar e desativar serviço no salão com isolamento por business_id")
    void shouldCreateListUpdateAndSoftDeleteServiceSuccessfully() throws Exception {
        // Arrange
        UserEntity owner = createTestUser("service.owner@perola.cv", "Owner Serviços");
        BusinessEntity business = createTestBusiness(owner, "Salão Glamour", "São Vicente", "Mindelo");
        String token = generateToken(owner, List.of(new TenantMembership(business.getId(), "OWNER")));

        CreateServiceRequest createRequest = new CreateServiceRequest(
                "Corte com Hidratação",
                "Corte premium com tratamento reconstrutor",
                60,
                new BigDecimal("2500.00")
        );

        // Act 1: Criar serviço
        MvcResult createResult = mockMvc.perform(post("/api/v1/businesses/" + business.getId() + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Corte com Hidratação"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.price").value(2500.00))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String serviceIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        UUID serviceId = UUID.fromString(serviceIdStr);

        // Act 2: Listar serviços do salão
        mockMvc.perform(get("/api/v1/businesses/" + business.getId() + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Corte com Hidratação"));

        // Act 3: Atualizar serviço via PUT
        UpdateServiceRequest updateRequest = new UpdateServiceRequest(
                "Corte com Hidratação Profunda",
                "Tratamento com queratina e corte moderno",
                75,
                new BigDecimal("3000.00"),
                true
        );

        mockMvc.perform(put("/api/v1/businesses/" + business.getId() + "/services/" + serviceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Corte com Hidratação Profunda"))
                .andExpect(jsonPath("$.durationMinutes").value(75))
                .andExpect(jsonPath("$.price").value(3000.00));

        // Act 4: Desativar serviço (Soft Delete) via DELETE
        mockMvc.perform(delete("/api/v1/businesses/" + business.getId() + "/services/" + serviceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString()))
                .andExpect(status().isNoContent());

        // Assert no banco: serviço permanece persistido com is_active = false
        ServiceEntity deactivatedInDb = serviceRepository.findById(serviceId).orElseThrow();
        assertThat(deactivatedInDb.isActive()).isFalse();

        // Act 5: Listar serviços ativos do salão não deve mais incluir o desativado
        mockMvc.perform(get("/api/v1/businesses/" + business.getId() + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("BE-007: Deve rejeitar criação de serviço com nome duplicado ativo no mesmo salão (HTTP 409 Conflict)")
    void shouldRejectDuplicateServiceNameInSameSalon() throws Exception {
        // Arrange
        UserEntity owner = createTestUser("dup.owner@perola.cv", "Owner Duplicados");
        BusinessEntity business = createTestBusiness(owner, "Salão Duplicados", "Santiago", "Praia");
        String token = generateToken(owner, List.of(new TenantMembership(business.getId(), "OWNER")));

        CreateServiceRequest req1 = new CreateServiceRequest("Escova Progressiva", "Desc", 90, new BigDecimal("4000.00"));
        CreateServiceRequest req2 = new CreateServiceRequest("Escova Progressiva", "Outra desc", 60, new BigDecimal("3500.00"));

        mockMvc.perform(post("/api/v1/businesses/" + business.getId() + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Act & Assert: Segunda criação com mesmo nome deve falhar com 409
        mockMvc.perform(post("/api/v1/businesses/" + business.getId() + "/services")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/conflict"));
    }

    @Test
    @DisplayName("BE-007: Deve adicionar profissional à equipa e atribuir serviços com sucesso")
    void shouldAddEmployeeAndAssignServicesSuccessfully() throws Exception {
        // Arrange
        UserEntity owner = createTestUser("team.owner@perola.cv", "Owner Equipa");
        BusinessEntity business = createTestBusiness(owner, "Salão Equipa", "Sal", "Santa Maria");
        String ownerToken = generateToken(owner, List.of(new TenantMembership(business.getId(), "OWNER")));

        // Criar 2 serviços
        ServiceEntity s1 = serviceRepository.save(ServiceEntity.builder()
                .businessId(business.getId())
                .name("Unhas de Gel")
                .durationMinutes(60)
                .price(new BigDecimal("2200.00"))
                .isActive(true)
                .build());

        ServiceEntity s2 = serviceRepository.save(ServiceEntity.builder()
                .businessId(business.getId())
                .name("Pedicure Spa")
                .durationMinutes(45)
                .price(new BigDecimal("1800.00"))
                .isActive(true)
                .build());

        // Criar um novo utilizador para ser colaborador
        UserEntity employeeUser = createTestUser("claudia.staff@perola.cv", "Cláudia Staff");

        // Act 1: Adicionar colaborador à equipa
        CreateEmployeeRequest empRequest = new CreateEmployeeRequest(employeeUser.getId(), "Cláudia Especialista em Unhas");
        MvcResult empResult = mockMvc.perform(post("/api/v1/businesses/" + business.getId() + "/employees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.displayName").value("Cláudia Especialista em Unhas"))
                .andExpect(jsonPath("$.userId").value(employeeUser.getId().toString()))
                .andReturn();

        UUID employeeId = UUID.fromString(objectMapper.readTree(empResult.getResponse().getContentAsString()).get("id").asText());

        // Assert: Papel EMPLOYEE deve ter sido atribuído ao utilizador
        assertThat(userRoleRepository.findAllByUserId(employeeUser.getId()))
                .anyMatch(r -> r.getRole() == UserRole.EMPLOYEE);

        // Act 2: Atribuir serviços ao colaborador
        AssignEmployeeServicesRequest assignRequest = new AssignEmployeeServicesRequest(List.of(s1.getId(), s2.getId()));
        mockMvc.perform(put("/api/v1/businesses/" + business.getId() + "/employees/" + employeeId + "/services")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", business.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.services.length()").value(2));

        // Act 3: Consultar serviços atribuídos
        mockMvc.perform(get("/api/v1/businesses/" + business.getId() + "/employees/" + employeeId + "/services")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Business-Id", business.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services.length()").value(2));
    }

    private BusinessEntity createTestBusiness(UserEntity owner, String name, String island, String city) {
        BusinessEntity business = BusinessEntity.builder()
                .ownerId(owner.getId())
                .name(name)
                .island(island)
                .city(city)
                .location("Avenida Central")
                .contactPhone("+2382601122")
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        return businessRepository.save(business);
    }
}

