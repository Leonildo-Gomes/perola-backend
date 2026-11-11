package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessCategoryItemRequest;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.CategoryRepository;
import cv.perola.modules.business.repository.EmployeeRepository;
import cv.perola.modules.business.validator.business.BusinessCategoryValidator;
import cv.perola.modules.business.validator.business.CreateBusinessValidator;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBusinessUseCaseTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessCategoryRepository businessCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountService userAccountService;

    @Spy
    private BusinessMapper businessMapper = new BusinessMapper();

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper();

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    private BusinessCategoryValidator businessCategoryValidator;
    private CreateBusinessValidator createBusinessValidator;
    private CreateBusinessUseCase createBusinessUseCase;

    @BeforeEach
    void setUp() {
        businessCategoryValidator = new BusinessCategoryValidator(categoryRepository);
        createBusinessValidator = new CreateBusinessValidator(businessRepository, businessCategoryValidator);
        createBusinessUseCase = new CreateBusinessUseCase(
                businessRepository,
                businessCategoryRepository,
                employeeRepository,
                userAccountService,
                businessMapper,
                categoryMapper,
                employeeMapper,
                createBusinessValidator
        );
    }

    @Test
    @DisplayName("Deve criar salão misto com categorias, atribuir papéis OWNER/EMPLOYEE e criar funcionário inicial")
    void shouldCreateBusinessAndAssignOwnerRoleAndCreateInitialEmployee() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID catHairId = UUID.randomUUID();
        UUID catNailsId = UUID.randomUUID();

        CategoryEntity catHair = CategoryEntity.builder().slug("hair").name("Cabeleireiro & Tranças").icon("Sparkles").displayOrder(2).isActive(true).build();
        catHair.setId(catHairId);
        CategoryEntity catNails = CategoryEntity.builder().slug("nails").name("Unhas (Manicure & Pedicure)").icon("Hand").displayOrder(3).isActive(true).build();
        catNails.setId(catNailsId);

        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Beleza Pura",
                "Santiago",
                "Praia",
                "Achada Santo António, Rua 5",
                "+2389912233",
                null,
                List.of(
                        new BusinessCategoryItemRequest(catHairId, true),
                        new BusinessCategoryItemRequest(catNailsId, false)
                )
        );

        when(businessRepository.existsByOwnerId(userId)).thenReturn(false);
        when(categoryRepository.findAllByIdInAndIsActiveTrue(Set.of(catHairId, catNailsId)))
                .thenReturn(List.of(catHair, catNails));
        when(userAccountService.findUserNameById(userId)).thenReturn(Optional.of("Carlos Mendes"));

        BusinessEntity savedBusiness = BusinessEntity.builder()
                .ownerId(userId)
                .name(request.name())
                .island(request.island())
                .city(request.city())
                .location(request.location())
                .contactPhone(request.contactPhone())
                .timezone("Atlantic/Cape_Verde")
                .isActive(true)
                .build();
        savedBusiness.setId(businessId);
        savedBusiness.setCreatedAt(Instant.now());
        savedBusiness.setUpdatedAt(Instant.now());

        when(businessRepository.save(any(BusinessEntity.class))).thenReturn(savedBusiness);
        when(businessCategoryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BusinessResponse response = createBusinessUseCase.execute(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(businessId);
        assertThat(response.ownerId()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("Salão Beleza Pura");
        assertThat(response.timezone()).isEqualTo("Atlantic/Cape_Verde");
        assertThat(response.isActive()).isTrue();
        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories().get(0).isPrimary()).isTrue();
        assertThat(response.categories().get(0).slug()).isEqualTo("hair");

        ArgumentCaptor<BusinessEntity> businessCaptor = ArgumentCaptor.forClass(BusinessEntity.class);
        verify(businessRepository).save(businessCaptor.capture());
        BusinessEntity capturedBusiness = businessCaptor.getValue();
        assertThat(capturedBusiness.getOwnerId()).isEqualTo(userId);
        assertThat(capturedBusiness.getTimezone()).isEqualTo("Atlantic/Cape_Verde");

        verify(userAccountService).assignRole(userId, UserRole.OWNER);
        verify(userAccountService).assignRole(userId, UserRole.EMPLOYEE);

        ArgumentCaptor<EmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        EmployeeEntity capturedEmployee = employeeCaptor.getValue();
        assertThat(capturedEmployee.getBusinessId()).isEqualTo(businessId);
        assertThat(capturedEmployee.getUserId()).isEqualTo(userId);
        assertThat(capturedEmployee.getDisplayName()).isEqualTo("Carlos Mendes");
        assertThat(capturedEmployee.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o proprietário já possuir um negócio registado")
    void shouldThrowConflictExceptionWhenOwnerAlreadyHasBusiness() {
        // Arrange
        UUID userId = UUID.randomUUID();
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Segundo Salão",
                "São Vicente",
                "Mindelo",
                "Rua de Lisboa",
                "+2389923344",
                "Atlantic/Cape_Verde",
                List.of(new BusinessCategoryItemRequest(UUID.randomUUID(), true))
        );
        when(businessRepository.existsByOwnerId(userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createBusinessUseCase.execute(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No MVP é permitido apenas 1 negócio por proprietário");

        verify(businessRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
        verify(userAccountService, never()).assignRole(any(), any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o utilizador não existir no Identity")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("hair").name("Cabeleireiro").build();
        category.setId(catId);

        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Teste",
                "Sal",
                "Santa Maria",
                "Centro",
                "+2389934455",
                null,
                List.of(new BusinessCategoryItemRequest(catId, true))
        );
        when(businessRepository.existsByOwnerId(userId)).thenReturn(false);
        when(categoryRepository.findAllByIdInAndIsActiveTrue(Set.of(catId))).thenReturn(List.of(category));
        when(userAccountService.findUserNameById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createBusinessUseCase.execute(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilizador não encontrado");

        verify(businessRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar AuthenticationFailedException quando o userId for nulo")
    void shouldThrowAuthenticationFailedExceptionWhenUserIdIsNull() {
        // Arrange
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Anónimo",
                "Santiago",
                "Praia",
                "Platô",
                "+2389945566",
                null,
                List.of(new BusinessCategoryItemRequest(UUID.randomUUID(), true))
        );

        // Act & Assert
        assertThatThrownBy(() -> createBusinessUseCase.execute(null, request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Autenticação necessária");

        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve respeitar o fuso horário customizado fornecido no pedido")
    void shouldRespectCustomTimezoneWhenProvided() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().slug("barbershop").name("Barbearia").displayOrder(1).build();
        category.setId(catId);

        CreateBusinessRequest request = new CreateBusinessRequest(
                "Salão Internacional",
                "Sal",
                "Espargos",
                "Avenida Principal",
                "+2389956677",
                "UTC",
                List.of(new BusinessCategoryItemRequest(catId, true))
        );

        when(businessRepository.existsByOwnerId(userId)).thenReturn(false);
        when(categoryRepository.findAllByIdInAndIsActiveTrue(Set.of(catId))).thenReturn(List.of(category));
        when(userAccountService.findUserNameById(userId)).thenReturn(Optional.of("Ana Santos"));

        BusinessEntity saved = BusinessEntity.builder()
                .ownerId(userId)
                .name(request.name())
                .island(request.island())
                .city(request.city())
                .location(request.location())
                .contactPhone(request.contactPhone())
                .timezone("UTC")
                .isActive(true)
                .build();
        saved.setId(UUID.randomUUID());
        when(businessRepository.save(any(BusinessEntity.class))).thenReturn(saved);
        when(businessCategoryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BusinessResponse response = createBusinessUseCase.execute(userId, request);

        // Assert
        assertThat(response.timezone()).isEqualTo("UTC");
    }
}
