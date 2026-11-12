package cv.perola.modules.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.security.JwtService;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.storage.FileStoragePort;
import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.dto.UpdateEmployeeRequest;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.CategoryRepository;
import cv.perola.modules.business.repository.MediaFileRepository;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.dto.UpdateUserProfileRequest;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MediaIntegrationTest {

        private static final byte[] VALID_WEBP_BYTES = new byte[] {
                        'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };

        private static final byte[] VALID_JPEG_BYTES = new byte[] {
                        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01
        };

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
        private CategoryRepository categoryRepository;

        @Autowired
        private BusinessCategoryRepository businessCategoryRepository;

        @Autowired
        private MediaFileRepository mediaFileRepository;

        @Autowired
        private FileStoragePort fileStoragePort;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        @DisplayName("BE-020: Deve fazer upload de imagem válida, gravar em media_files e servir em GET /api/v1/media/** com Cache-Control imutável")
        void shouldUploadValidImageAndServeWithImmutableCacheControl() throws Exception {
                // Arrange
                UserEntity owner = createTestUser("media.owner@perola.cv", "Owner Media");
                BusinessEntity business = createTestBusiness(owner, "Salão Media", "Santiago", "Praia");
                String ownerToken = generateToken(owner,
                                List.of(new TenantMembership(business.getId(), UserRole.OWNER.name())));

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "corte-moderno.webp",
                                "image/webp",
                                VALID_WEBP_BYTES);

                String storageKey = null;
                try {
                        // Act 1: Upload multipart POST /api/v1/media/upload
                        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/media/upload")
                                        .file(file)
                                        .param("scope", "SERVICE_IMAGE")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .header("X-Business-Id", business.getId().toString()))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").isNotEmpty())
                                        .andExpect(jsonPath("$.scope").value("SERVICE_IMAGE"))
                                        .andExpect(jsonPath("$.contentType").value("image/webp"))
                                        .andExpect(jsonPath("$.sizeBytes").value(VALID_WEBP_BYTES.length))
                                        .andReturn();

                        JsonNode json = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
                        UUID mediaId = UUID.fromString(json.get("id").asText());
                        storageKey = json.get("storageKey").asText();
                        String url = json.get("url").asText();

                        assertThat(storageKey).startsWith("businesses/" + business.getId() + "/services/")
                                        .endsWith(".webp");
                        assertThat(url).endsWith("/api/v1/media/" + storageKey);

                        // Assert no banco: registo em media_files
                        MediaFileEntity savedMedia = mediaFileRepository.findById(mediaId).orElseThrow();
                        assertThat(savedMedia.getBusinessId()).isEqualTo(business.getId());
                        assertThat(savedMedia.getUploadedById()).isEqualTo(owner.getId());
                        assertThat(savedMedia.getScope()).isEqualTo(MediaScope.SERVICE_IMAGE);
                        assertThat(savedMedia.getStorageKey()).isEqualTo(storageKey);

                        // Act 2: Leitura pública sem JWT via GET /api/v1/media/{*storageKey}
                        mockMvc.perform(get("/api/v1/media/" + storageKey))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                                                        "public, max-age=31536000, immutable"))
                                        .andExpect(content().contentType("image/webp"))
                                        .andExpect(content().bytes(VALID_WEBP_BYTES));
                } finally {
                        if (storageKey != null) {
                                fileStoragePort.delete(storageKey);
                        }
                }
        }

        @Test
        @DisplayName("BE-020: Deve rejeitar upload de script/executável disfarçado de .jpg ou ficheiro > 5 MB com 400 Bad Request (RFC 7807)")
        void shouldRejectDisguisedExecutableAndOversizedFilesWithBadRequest() throws Exception {
                // Arrange
                UserEntity customer = createTestUser("customer.media@perola.cv", "Cliente Media");
                String token = generateToken(customer, List.of());

                MockMultipartFile disguisedScript = new MockMultipartFile(
                                "file",
                                "exploit.jpg",
                                "image/jpeg",
                                "#!/bin/bash\necho pwned".getBytes(StandardCharsets.UTF_8));

                byte[] oversizedBytes = new byte[5_242_881];
                System.arraycopy(VALID_JPEG_BYTES, 0, oversizedBytes, 0, VALID_JPEG_BYTES.length);
                MockMultipartFile oversizedFile = new MockMultipartFile(
                                "file",
                                "huge.jpg",
                                "image/jpeg",
                                oversizedBytes);

                // Act & Assert 1: Script disfarçado de .jpg -> 400 Bad Request
                mockMvc.perform(multipart("/api/v1/media/upload")
                                .file(disguisedScript)
                                .param("scope", "USER_AVATAR")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                                .andExpect(jsonPath("$.status").value(400));

                // Act & Assert 2: Ficheiro > 5 MB -> 400 Bad Request
                mockMvc.perform(multipart("/api/v1/media/upload")
                                .file(oversizedFile)
                                .param("scope", "USER_AVATAR")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/validation-error"))
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("BE-020: Deve rejeitar upload em BUSINESS_LOGO, SERVICE_IMAGE ou EMPLOYEE_AVATAR sem X-Business-Id válido do salão com 403 Forbidden")
        void shouldRejectBusinessScopesWithoutValidOwnerBusinessHeaderWithForbidden() throws Exception {
                // Arrange
                UserEntity customer = createTestUser("unauth.media@perola.cv", "Cliente Sem Salão");
                String customerToken = generateToken(customer, List.of());
                MockMultipartFile file = new MockMultipartFile("file", "logo.webp", "image/webp", VALID_WEBP_BYTES);

                // Act & Assert 1: Sem X-Business-Id -> 403 Forbidden
                for (String scope : List.of("BUSINESS_LOGO", "SERVICE_IMAGE", "EMPLOYEE_AVATAR")) {
                        mockMvc.perform(multipart("/api/v1/media/upload")
                                        .file(file)
                                        .param("scope", scope)
                                        .header("Authorization", "Bearer " + customerToken))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.type")
                                                        .value("https://perola.cv/errors/tenant-access-denied"))
                                        .andExpect(jsonPath("$.status").value(403));
                }

                // Act & Assert 2: Com X-Business-Id de outro salão -> 403 Forbidden
                mockMvc.perform(multipart("/api/v1/media/upload")
                                .file(file)
                                .param("scope", "BUSINESS_LOGO")
                                .header("Authorization", "Bearer " + customerToken)
                                .header("X-Business-Id", UUID.randomUUID().toString()))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.type").value("https://perola.cv/errors/tenant-access-denied"))
                                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("BE-020: Deve persistir e devolver logoUrl em Business, imageUrl em Service, avatarUrl em Employee e avatarUrl em PATCH /api/v1/users/me")
        void shouldPersistAndReturnImageUrlsAcrossUserBusinessServiceAndEmployee() throws Exception {
                // Arrange
                UserEntity user = createTestUser("fullflow.owner@perola.cv", "Dona Sofia");
                String initialToken = generateToken(user, List.of());
                CategoryEntity hairCat = categoryRepository.findBySlug("hair").orElseThrow();

                // Act 1: PATCH /api/v1/users/me com avatarUrl
                String userAvatarUrl = "http://localhost:8080/api/v1/media/users/" + user.getId()
                                + "/avatar/sofia.webp";
                UpdateUserProfileRequest updateUserReq = new UpdateUserProfileRequest(
                                "Sofia Andrade",
                                "+2389988776",
                                userAvatarUrl);
                mockMvc.perform(patch("/api/v1/users/me")
                                .header("Authorization", "Bearer " + initialToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateUserReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Sofia Andrade"))
                                .andExpect(jsonPath("$.phone").value("+2389988776"))
                                .andExpect(jsonPath("$.avatarUrl").value(userAvatarUrl));

                // Act 2: POST /api/v1/businesses e PATCH /api/v1/businesses/{id} com logoUrl
                String initialLogoUrl = "http://localhost:8080/api/v1/media/businesses/temp/logo/logo1.webp";
                CreateBusinessRequest createBizReq = new CreateBusinessRequest(
                                "Salão Sofia Beauty",
                                "Santiago",
                                "Praia",
                                "Achada Santo António",
                                "+2382623344",
                                initialLogoUrl,
                                "Atlantic/Cape_Verde",
                                List.of(new BusinessCategoryItemRequest(hairCat.getId(), true)));
                MvcResult bizResult = mockMvc.perform(post("/api/v1/businesses")
                                .header("Authorization", "Bearer " + initialToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBizReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.logoUrl").value(initialLogoUrl))
                                .andReturn();

                UUID businessId = UUID.fromString(
                                objectMapper.readTree(bizResult.getResponse().getContentAsString()).get("id").asText());
                String ownerToken = generateToken(user,
                                List.of(new TenantMembership(businessId, UserRole.OWNER.name())));

                String updatedLogoUrl = "http://localhost:8080/api/v1/media/businesses/" + businessId
                                + "/logo/logo2.webp";
                UpdateBusinessRequest updateBizReq = new UpdateBusinessRequest(
                                null, null, null, null, null, updatedLogoUrl, null, null);
                mockMvc.perform(patch("/api/v1/businesses/" + businessId)
                                .header("Authorization", "Bearer " + ownerToken)
                                .header("X-Business-Id", businessId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateBizReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.logoUrl").value(updatedLogoUrl));

                // Act 3: POST e PATCH /api/v1/businesses/{id}/services com imageUrl
                String serviceImgUrl = "http://localhost:8080/api/v1/media/businesses/" + businessId
                                + "/services/srv1.webp";
                CreateServiceRequest createSrvReq = new CreateServiceRequest(
                                hairCat.getId(),
                                "Tranças Box Braids",
                                "Tranças completas",
                                120,
                                new BigDecimal("4500.00"),
                                serviceImgUrl);
                MvcResult srvResult = mockMvc.perform(post("/api/v1/businesses/" + businessId + "/services")
                                .header("Authorization", "Bearer " + ownerToken)
                                .header("X-Business-Id", businessId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createSrvReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.imageUrl").value(serviceImgUrl))
                                .andReturn();

                UUID serviceId = UUID.fromString(
                                objectMapper.readTree(srvResult.getResponse().getContentAsString()).get("id").asText());
                String updatedServiceImgUrl = "http://localhost:8080/api/v1/media/businesses/" + businessId
                                + "/services/srv2.webp";
                UpdateServiceRequest updateSrvReq = new UpdateServiceRequest(
                                null, null, null, null, null, updatedServiceImgUrl, null);
                mockMvc.perform(patch("/api/v1/businesses/" + businessId + "/services/" + serviceId)
                                .header("Authorization", "Bearer " + ownerToken)
                                .header("X-Business-Id", businessId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateSrvReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.imageUrl").value(updatedServiceImgUrl));

                // Act 4: POST e PATCH /api/v1/businesses/{id}/employees com avatarUrl
                String empAvatarUrl = "http://localhost:8080/api/v1/media/businesses/" + businessId
                                + "/employees/emp1.webp";
                CreateEmployeeRequest createEmpReq = new CreateEmployeeRequest(
                                null,
                                "Carla Trancista",
                                "Especialista em Box Braids",
                                empAvatarUrl);
                MvcResult empResult = mockMvc.perform(post("/api/v1/businesses/" + businessId + "/employees")
                                .header("Authorization", "Bearer " + ownerToken)
                                .header("X-Business-Id", businessId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createEmpReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.avatarUrl").value(empAvatarUrl))
                                .andReturn();

                UUID employeeId = UUID.fromString(
                                objectMapper.readTree(empResult.getResponse().getContentAsString()).get("id").asText());
                String updatedEmpAvatarUrl = "http://localhost:8080/api/v1/media/businesses/" + businessId
                                + "/employees/emp2.webp";
                UpdateEmployeeRequest updateEmpReq = new UpdateEmployeeRequest(
                                null, null, updatedEmpAvatarUrl, null);
                mockMvc.perform(patch("/api/v1/businesses/" + businessId + "/employees/" + employeeId)
                                .header("Authorization", "Bearer " + ownerToken)
                                .header("X-Business-Id", businessId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateEmpReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.avatarUrl").value(updatedEmpAvatarUrl));
        }

        private UserEntity createTestUser(String email, String name) {
                UserEntity user = userRepository.save(UserEntity.builder()
                                .name(name)
                                .email(email)
                                .phone("+23899" + (int) (Math.random() * 90000 + 10000))
                                .passwordHash("hashed")
                                .isActive(true)
                                .build());
                userRoleRepository.save(UserRoleEntity.builder()
                                .userId(user.getId())
                                .role(UserRole.CUSTOMER)
                                .build());
                return user;
        }

        private BusinessEntity createTestBusiness(UserEntity owner, String name, String island, String city) {
                BusinessEntity business = businessRepository.save(BusinessEntity.builder()
                                .ownerId(owner.getId())
                                .name(name)
                                .island(island)
                                .city(city)
                                .location("Avenida Central")
                                .contactPhone("+2382601122")
                                .timezone("Atlantic/Cape_Verde")
                                .isActive(true)
                                .build());
                CategoryEntity hairCat = categoryRepository.findBySlug("hair").orElseThrow();
                businessCategoryRepository.save(BusinessCategoryEntity.builder()
                                .businessId(business.getId())
                                .categoryId(hairCat.getId())
                                .category(hairCat)
                                .isPrimary(true)
                                .build());
                return business;
        }

        private String generateToken(UserEntity user, List<TenantMembership> memberships) {
                List<String> roles = List.of("CUSTOMER", "OWNER", "EMPLOYEE");
                return jwtService.generateAccessToken(user.getId(), user.getEmail(), roles, memberships);
        }
}