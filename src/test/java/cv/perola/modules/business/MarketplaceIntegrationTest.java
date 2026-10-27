package cv.perola.modules.business;

import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MarketplaceIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("BE-008: Deve pesquisar salões públicos sem token JWT e sem X-Business-Id com filtros por ilha e cidade")
    void shouldSearchMarketplaceBusinessesWithoutAuthentication() throws Exception {
        // Arrange
        UserEntity owner1 = createTestUser("owner.market1@perola.cv", "Owner 1");
        BusinessEntity b1 = createTestBusiness(owner1, "Salão Estilo Praia", "Santiago", "Praia", true);

        UserEntity owner2 = createTestUser("owner.market2@perola.cv", "Owner 2");
        BusinessEntity b2 = createTestBusiness(owner2, "Salão Estilo Mindelo", "São Vicente", "Mindelo", true);

        UserEntity owner3 = createTestUser("owner.market3@perola.cv", "Owner 3");
        BusinessEntity b3Inactive = createTestBusiness(owner3, "Salão Inativo Praia", "Santiago", "Praia", false);

        // Criar 2 serviços para b1
        createTestService(b1.getId(), "Corte Praia 1", new BigDecimal("1000.00"), true);
        createTestService(b1.getId(), "Corte Praia 2", new BigDecimal("1500.00"), true);

        // Act & Assert 1: Busca sem filtros deve retornar os ativos e ignorar inativos
        mockMvc.perform(get("/api/v1/marketplace/businesses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Estilo Praia')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Estilo Mindelo')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Inativo Praia')]").doesNotExist());

        // Act & Assert 2: Filtrar por ilha Santiago e cidade Praia
        mockMvc.perform(get("/api/v1/marketplace/businesses")
                        .param("island", "Santiago")
                        .param("city", "Praia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Estilo Praia')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Estilo Mindelo')]").doesNotExist());

        // Act & Assert 3: Filtrar por termo de busca
        mockMvc.perform(get("/api/v1/marketplace/businesses")
                        .param("search", "Mindelo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Estilo Mindelo')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Salão Estilo Praia')]").doesNotExist());
    }

    @Test
    @DisplayName("BE-008: Deve consultar detalhes públicos de salão ativo incluindo serviços e colaboradores")
    void shouldGetPublicBusinessDetail() throws Exception {
        // Arrange
        UserEntity owner = createTestUser("owner.detail@perola.cv", "Owner Detail");
        BusinessEntity business = createTestBusiness(owner, "Salão Morabeza VIP", "Sal", "Santa Maria", true);

        createTestService(business.getId(), "Escova VIP", new BigDecimal("2000.00"), true);
        createTestService(business.getId(), "Serviço Desativado", new BigDecimal("1000.00"), false);

        createTestEmployee(business.getId(), "Rita Profissional", true);
        createTestEmployee(business.getId(), "Colaborador Inativo", false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/marketplace/businesses/" + business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(business.getId().toString()))
                .andExpect(jsonPath("$.name").value("Salão Morabeza VIP"))
                .andExpect(jsonPath("$.island").value("Sal"))
                .andExpect(jsonPath("$.city").value("Santa Maria"))
                .andExpect(jsonPath("$.services.length()").value(1))
                .andExpect(jsonPath("$.services[0].name").value("Escova VIP"))
                .andExpect(jsonPath("$.employees.length()").value(1))
                .andExpect(jsonPath("$.employees[0].displayName").value("Rita Profissional"));
    }

    @Test
    @DisplayName("BE-008: Deve retornar 404 Not Found ao consultar detalhes de salão inexistente ou inativo")
    void shouldReturn404WhenBusinessNotFoundOrInactive() throws Exception {
        // Arrange
        UserEntity owner = createTestUser("owner.inactive@perola.cv", "Owner Inactive");
        BusinessEntity inactiveBusiness = createTestBusiness(owner, "Salão Fechado", "Santiago", "Praia", false);

        // Act & Assert 1: Inativo
        mockMvc.perform(get("/api/v1/marketplace/businesses/" + inactiveBusiness.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/resource-not-found"));

        // Act & Assert 2: Inexistente
        mockMvc.perform(get("/api/v1/marketplace/businesses/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("BE-008: Deve pesquisar serviços no marketplace público ignorando serviços inativos e salões inativos")
    void shouldSearchMarketplaceServicesFilteringOutInactives() throws Exception {
        // Arrange
        UserEntity ownerActive = createTestUser("owner.active.svc@perola.cv", "Owner Active");
        BusinessEntity bActive = createTestBusiness(ownerActive, "Salão Activo", "Santiago", "Praia", true);

        UserEntity ownerInactive = createTestUser("owner.inactive.svc@perola.cv", "Owner Inactive");
        BusinessEntity bInactive = createTestBusiness(ownerInactive, "Salão Inactivo", "Santiago", "Praia", false);

        createTestService(bActive.getId(), "Coloração Total", new BigDecimal("3500.00"), true);
        createTestService(bActive.getId(), "Coloração Inativa", new BigDecimal("3000.00"), false);
        createTestService(bInactive.getId(), "Coloração Salão Inativo", new BigDecimal("2500.00"), true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/marketplace/services")
                        .param("search", "Coloração"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Coloração Total"))
                .andExpect(jsonPath("$.content[0].businessName").value("Salão Activo"))
                .andExpect(jsonPath("$.content[0].businessIsland").value("Santiago"))
                .andExpect(jsonPath("$.content[0].price").value(3500.00));
    }

    private UserEntity createTestUser(String email, String name) {
        return userRepository.save(UserEntity.builder()
                .email(email)
                .name(name)
                .phone("+2389912233")
                .passwordHash("hash")
                .isActive(true)
                .build());
    }

    private BusinessEntity createTestBusiness(UserEntity owner, String name, String island, String city, boolean isActive) {
        return businessRepository.save(BusinessEntity.builder()
                .ownerId(owner.getId())
                .name(name)
                .island(island)
                .city(city)
                .location("Rua Central")
                .contactPhone("+2382600011")
                .timezone("Atlantic/Cape_Verde")
                .isActive(isActive)
                .build());
    }

    private ServiceEntity createTestService(UUID businessId, String name, BigDecimal price, boolean isActive) {
        return serviceRepository.save(ServiceEntity.builder()
                .businessId(businessId)
                .name(name)
                .durationMinutes(45)
                .price(price)
                .isActive(isActive)
                .build());
    }

    private EmployeeEntity createTestEmployee(UUID businessId, String displayName, boolean isActive) {
        return employeeRepository.save(EmployeeEntity.builder()
                .businessId(businessId)
                .displayName(displayName)
                .isActive(isActive)
                .build());
    }
}
