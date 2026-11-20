package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.exception.PendingAppointmentsLimitExceededException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.AppointmentHistoryEntity;
import cv.perola.modules.booking.domain.BusinessCustomerEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentHistoryRepository;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.repository.BusinessCustomerRepository;
import cv.perola.modules.booking.validator.appointment.CreateAppointmentValidator;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.service.CustomerResolutionService;
import cv.perola.modules.identity.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentUseCaseTest {

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private AppointmentHistoryRepository appointmentHistoryRepository;

        @Mock
        private BusinessCustomerRepository businessCustomerRepository;

        @Mock
        private CustomerResolutionService customerResolutionService;

        @Mock
        private BusinessEmployeeQueryService businessEmployeeQueryService;

        @Mock
        private UserAccountService userAccountService;

        @Mock
        private AppointmentMapper appointmentMapper;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        private CreateAppointmentValidator createAppointmentValidator;
        private CreateAppointmentUseCase createAppointmentUseCase;

        @BeforeEach
        void setUp() {
                createAppointmentValidator = new CreateAppointmentValidator(businessEmployeeQueryService, userAccountService);
                createAppointmentUseCase = new CreateAppointmentUseCase(
                                appointmentRepository,
                                appointmentHistoryRepository,
                                businessCustomerRepository,
                                customerResolutionService,
                                appointmentMapper,
                                eventPublisher,
                                createAppointmentValidator
                );
        }

        @Test
        @DisplayName("Cenário 1: Deve criar agendamento Guest com sucesso (201 Created, createdById = null, tier = NEW)")
        void shouldCreateGuestAppointmentSuccessfullyWhenUnauthenticated() {
                // Arrange
                UUID businessId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();
                UUID appointmentId = UUID.randomUUID();

                OffsetDateTime startOffset = OffsetDateTime.of(2026, 11, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
                Instant expectedStart = Instant.parse("2026-11-03T11:00:00Z");
                Instant expectedEnd = Instant.parse("2026-11-03T12:00:00Z");

                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId,
                                employeeId,
                                serviceId,
                                startOffset,
                                "Primeira visita",
                                "Ana Silva",
                                "ana@cv.com",
                                "+2389912345"
                );

                CustomerEntity guestCustomer = CustomerEntity.builder()
                                .name("Ana Silva")
                                .email("ana@cv.com")
                                .phone("+2389912345")
                                .userId(null)
                                .build();
                guestCustomer.setId(customerId);

                BusinessResponse business = new BusinessResponse(
                                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                                "Platô", "+2382610000", "Atlantic/Cape_Verde", List.of(), true, Instant.now(), Instant.now());
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, UUID.randomUUID(), "Carla", null, true, Instant.now(), Instant.now());
                ServiceResponse service = new ServiceResponse(
                                serviceId, businessId, UUID.randomUUID(), "hair", "Cabeleireiro & Tranças",
                                "Corte & Brushing", "Descrição", 60,
                                new BigDecimal("2500.00"), true, List.of(), Instant.now(), Instant.now());

                AppointmentEntity mappedEntity = AppointmentEntity.builder()
                                .businessId(businessId)
                                .customerId(customerId)
                                .employeeId(employeeId)
                                .serviceId(serviceId)
                                .createdById(null)
                                .startTime(expectedStart)
                                .endTime(expectedEnd)
                                .serviceName("Corte & Brushing")
                                .servicePrice(new BigDecimal("2500.00"))
                                .serviceDuration(60)
                                .status(AppointmentStatus.PENDING)
                                .notes("Primeira visita")
                                .build();
                mappedEntity.setId(appointmentId);

                AppointmentHistoryEntity historyEntity = AppointmentHistoryEntity.builder()
                                .id(UUID.randomUUID())
                                .appointmentId(appointmentId)
                                .performedById(null)
                                .action(AppointmentAction.CREATED)
                                .toStatus(AppointmentStatus.PENDING)
                                .newStartTime(expectedStart)
                                .newEndTime(expectedEnd)
                                .notes("Primeira visita")
                                .build();

                AppointmentResponse expectedResponse = new AppointmentResponse(
                                appointmentId, businessId, "Salão Pérola", customerId, "Ana Silva", "ana@cv.com", "+2389912345",
                                employeeId, "Carla", serviceId, "Corte & Brushing", new BigDecimal("2500.00"), 60,
                                startOffset, startOffset.plusMinutes(60), null, null, AppointmentStatus.PENDING,
                                null, null, "Primeira visita", startOffset, List.of()
                );

                when(customerResolutionService.resolveForBooking("Ana Silva", "ana@cv.com", "+2389912345", null))
                                .thenReturn(guestCustomer);
                when(appointmentRepository.countByBusinessIdAndCustomerIdAndStatusIn(eq(businessId), eq(customerId), anyList()))
                                .thenReturn(0L);
                when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
                when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
                when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
                when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);
                when(businessCustomerRepository.existsByBusinessIdAndCustomerId(businessId, customerId)).thenReturn(false);
                when(appointmentMapper.toEntity(request, customerId, null, service, expectedStart, expectedEnd)).thenReturn(mappedEntity);
                when(appointmentRepository.saveAndFlush(mappedEntity)).thenReturn(mappedEntity);
                when(appointmentMapper.toHistoryEntity(eq(appointmentId), eq(null), eq(AppointmentAction.CREATED), eq(null), eq(AppointmentStatus.PENDING), any(), any(), eq(expectedStart), eq(expectedEnd), eq("Primeira visita"))).thenReturn(historyEntity);
                when(appointmentHistoryRepository.save(historyEntity)).thenReturn(historyEntity);
                when(appointmentMapper.toResponse(eq(mappedEntity), anyList(), any(ZoneId.class), eq("Salão Pérola"), eq("Ana Silva"), eq("ana@cv.com"), eq("+2389912345"), eq("Carla"), eq(null), eq(null)))
                                .thenReturn(expectedResponse);

                // Act
                AppointmentResponse result = createAppointmentUseCase.execute((UserPrincipal) null, request);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.status()).isEqualTo(AppointmentStatus.PENDING);
                assertThat(result.customerName()).isEqualTo("Ana Silva");

                ArgumentCaptor<BusinessCustomerEntity> bcCaptor = ArgumentCaptor.forClass(BusinessCustomerEntity.class);
                verify(businessCustomerRepository).save(bcCaptor.capture());
                assertThat(bcCaptor.getValue().getTier()).isEqualTo(CustomerTier.NEW);
                assertThat(bcCaptor.getValue().getBusinessId()).isEqualTo(businessId);
                assertThat(bcCaptor.getValue().getCustomerId()).isEqualTo(customerId);

                verify(appointmentRepository).saveAndFlush(mappedEntity);
                verify(eventPublisher).publishEvent(any(AppointmentLifecycleEvent.class));
        }

        @Test
        @DisplayName("Cenário 2: Deve lançar ValidationException quando visitante omitir dados de contacto obrigatórios")
        void shouldThrowValidationExceptionWhenGuestMissesContactDetails() {
                // Arrange
                CreateAppointmentRequest missingName = new CreateAppointmentRequest(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null,
                                "", "ana@cv.com", "+2389912345"
                );
                CreateAppointmentRequest missingEmail = new CreateAppointmentRequest(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null,
                                "Ana Silva", "", "+2389912345"
                );
                CreateAppointmentRequest invalidPhone = new CreateAppointmentRequest(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null,
                                "Ana Silva", "ana@cv.com", "12345"
                );

                // Act & Assert
                assertThatThrownBy(() -> createAppointmentUseCase.execute((UserPrincipal) null, missingName))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("nome do cliente é obrigatório");

                assertThatThrownBy(() -> createAppointmentUseCase.execute((UserPrincipal) null, missingEmail))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("email do cliente é obrigatório");

                assertThatThrownBy(() -> createAppointmentUseCase.execute((UserPrincipal) null, invalidPhone))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("telefone do cliente deve ser um número válido");

                verify(appointmentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Cenário 3: Deve associar createdById ao colaborador quando o agendamento for criado pelo Staff para walk-in")
        void shouldCreateAppointmentSuccessfullyByStaffForWalkInCustomer() {
                // Arrange
                UUID staffUserId = UUID.randomUUID();
                UserPrincipal staffPrincipal = new UserPrincipal(
                                staffUserId, "staff@salao.cv", "password", List.of("EMPLOYEE"), List.of(), true
                );

                UUID businessId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();

                OffsetDateTime startOffset = OffsetDateTime.of(2026, 11, 3, 14, 0, 0, 0, ZoneOffset.ofHours(-1));
                Instant expectedStart = Instant.parse("2026-11-03T15:00:00Z");
                Instant expectedEnd = Instant.parse("2026-11-03T16:00:00Z");

                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId, employeeId, serviceId, startOffset, "Marcação presencial balcão",
                                "João Santos", "joao@exemplo.cv", "+2389812345"
                );

                CustomerEntity walkInCustomer = CustomerEntity.builder()
                                .name("João Santos")
                                .email("joao@exemplo.cv")
                                .phone("+2389812345")
                                .userId(null)
                                .build();
                walkInCustomer.setId(customerId);

                BusinessResponse business = new BusinessResponse(
                                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                                "Platô", "+2382610000", "Atlantic/Cape_Verde", List.of(), true, Instant.now(), Instant.now());
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, UUID.randomUUID(), "Carla", null, true, Instant.now(), Instant.now());
                ServiceResponse service = new ServiceResponse(
                                serviceId, businessId, UUID.randomUUID(), "hair", "Corte",
                                "Corte Masculino", "Descrição", 60,
                                new BigDecimal("1500.00"), true, List.of(), Instant.now(), Instant.now());

                AppointmentEntity mappedEntity = AppointmentEntity.builder()
                                .businessId(businessId)
                                .customerId(customerId)
                                .employeeId(employeeId)
                                .serviceId(serviceId)
                                .createdById(staffUserId)
                                .startTime(expectedStart)
                                .endTime(expectedEnd)
                                .serviceName("Corte Masculino")
                                .servicePrice(new BigDecimal("1500.00"))
                                .serviceDuration(60)
                                .status(AppointmentStatus.PENDING)
                                .notes("Marcação presencial balcão")
                                .build();

                when(customerResolutionService.resolveForBooking("João Santos", "joao@exemplo.cv", "+2389812345", null))
                                .thenReturn(walkInCustomer);
                when(appointmentRepository.countByBusinessIdAndCustomerIdAndStatusIn(eq(businessId), eq(customerId), anyList()))
                                .thenReturn(0L);
                when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
                when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
                when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
                when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);
                when(businessCustomerRepository.existsByBusinessIdAndCustomerId(businessId, customerId)).thenReturn(true);
                when(appointmentMapper.toEntity(request, customerId, staffUserId, service, expectedStart, expectedEnd)).thenReturn(mappedEntity);
                when(appointmentRepository.saveAndFlush(mappedEntity)).thenReturn(mappedEntity);
                when(appointmentMapper.toHistoryEntity(any(), eq(staffUserId), any(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(AppointmentHistoryEntity.builder().build());

                // Act
                createAppointmentUseCase.execute(staffPrincipal, request);

                // Assert
                ArgumentCaptor<AppointmentEntity> captor = ArgumentCaptor.forClass(AppointmentEntity.class);
                verify(appointmentRepository).saveAndFlush(captor.capture());
                assertThat(captor.getValue().getCreatedById()).isEqualTo(staffUserId);
                assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Cenário 4: Bloqueio Anti-Spam com 429 Too Many Requests quando cliente já possui 3 marcações ativas")
        void shouldThrowPendingAppointmentsLimitExceededExceptionWhenCustomerHasThreeOrMoreActiveAppointments() {
                // Arrange
                UUID businessId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();

                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId, UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null,
                                "Spammer", "spam@cv.com", "+2389912345"
                );

                CustomerEntity customer = CustomerEntity.builder()
                                .name("Spammer")
                                .email("spam@cv.com")
                                .phone("+2389912345")
                                .build();
                customer.setId(customerId);

                when(customerResolutionService.resolveForBooking("Spammer", "spam@cv.com", "+2389912345", null))
                                .thenReturn(customer);
                when(appointmentRepository.countByBusinessIdAndCustomerIdAndStatusIn(eq(businessId), eq(customerId), anyList()))
                                .thenReturn(3L);

                // Act & Assert
                assertThatThrownBy(() -> createAppointmentUseCase.execute((UserPrincipal) null, request))
                                .isInstanceOf(PendingAppointmentsLimitExceededException.class)
                                .hasMessageContaining("limite máximo de 3 marcações");

                verify(appointmentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Deve permitir ao utilizador autenticado agendar para si próprio quando customerEmail for omitido")
        void shouldCreateAppointmentForAuthenticatedUserWithoutCustomerEmail() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UserPrincipal principal = new UserPrincipal(
                                userId, "cliente@cv.com", "pass", List.of("CUSTOMER"), List.of(), true
                );

                UUID businessId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();

                OffsetDateTime startOffset = OffsetDateTime.of(2026, 11, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId, employeeId, serviceId, startOffset, "Self-service booking",
                                null, null, null
                );

                CustomerEntity customer = CustomerEntity.builder()
                                .name("Cliente Autenticado")
                                .email("cliente@cv.com")
                                .phone("+2389911111")
                                .userId(userId)
                                .build();
                customer.setId(customerId);

                BusinessResponse business = new BusinessResponse(
                                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                                "Platô", "+2382610000", "Atlantic/Cape_Verde", List.of(), true, Instant.now(), Instant.now());
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, UUID.randomUUID(), "Carla", null, true, Instant.now(), Instant.now());
                ServiceResponse service = new ServiceResponse(
                                serviceId, businessId, UUID.randomUUID(), "hair", "Corte",
                                "Corte", "Descrição", 60,
                                new BigDecimal("2000.00"), true, List.of(), Instant.now(), Instant.now());

                AppointmentEntity mappedEntity = AppointmentEntity.builder()
                                .businessId(businessId)
                                .customerId(customerId)
                                .employeeId(employeeId)
                                .serviceId(serviceId)
                                .createdById(userId)
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .serviceName("Corte")
                                .servicePrice(new BigDecimal("2000.00"))
                                .serviceDuration(60)
                                .status(AppointmentStatus.PENDING)
                                .build();

                when(customerResolutionService.resolveForAuthenticatedUser(userId)).thenReturn(customer);
                when(appointmentRepository.countByBusinessIdAndCustomerIdAndStatusIn(eq(businessId), eq(customerId), anyList()))
                                .thenReturn(1L);
                when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
                when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
                when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
                when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);
                when(businessCustomerRepository.existsByBusinessIdAndCustomerId(businessId, customerId)).thenReturn(true);
                when(appointmentMapper.toEntity(eq(request), eq(customerId), eq(userId), eq(service), any(), any())).thenReturn(mappedEntity);
                when(appointmentRepository.saveAndFlush(mappedEntity)).thenReturn(mappedEntity);
                when(appointmentMapper.toHistoryEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(AppointmentHistoryEntity.builder().build());

                // Act
                createAppointmentUseCase.execute(principal, request);

                // Assert
                verify(customerResolutionService).resolveForAuthenticatedUser(userId);
                verify(appointmentRepository).saveAndFlush(mappedEntity);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o cliente não existir (sobrecarga por ID)")
        void shouldThrowResourceNotFoundExceptionWhenCustomerDoesNotExist() {
                // Arrange
                UUID customerId = UUID.randomUUID();
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null);
                when(userAccountService.existsById(customerId)).thenReturn(false);

                // Act & Assert
                assertThatThrownBy(() -> createAppointmentUseCase.execute(customerId, request))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessage("Cliente não encontrado.");
                verify(appointmentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o salão não existir")
        void shouldThrowResourceNotFoundExceptionWhenBusinessDoesNotExist() {
                // Arrange
                UUID customerId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId, UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null);
                when(userAccountService.existsById(customerId)).thenReturn(true);
                when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> createAppointmentUseCase.execute(customerId, request))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessage("Salão não encontrado.");
                verify(appointmentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o profissional não pertencer ao salão")
        void shouldThrowResourceNotFoundExceptionWhenEmployeeDoesNotBelongToBusiness() {
                // Arrange
                UUID customerId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId, employeeId, UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null);
                BusinessResponse business = new BusinessResponse(
                                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                                "Platô", "+2382610000", "Atlantic/Cape_Verde", List.of(), true, Instant.now(),
                                Instant.now());
                when(userAccountService.existsById(customerId)).thenReturn(true);
                when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
                when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> createAppointmentUseCase.execute(customerId, request))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessage("Profissional não encontrado para o salão especificado.");
                verify(appointmentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando o profissional não realizar o serviço selecionado")
        void shouldThrowValidationExceptionWhenServiceIsNotAssignedToEmployee() {
                // Arrange
                UUID customerId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID serviceId = UUID.randomUUID();
                CreateAppointmentRequest request = new CreateAppointmentRequest(
                                businessId, employeeId, serviceId,
                                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null);
                BusinessResponse business = new BusinessResponse(
                                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                                "Platô", "+2382610000", "Atlantic/Cape_Verde", List.of(), true, Instant.now(),
                                Instant.now());
                EmployeeResponse employee = new EmployeeResponse(
                                employeeId, businessId, UUID.randomUUID(), "Carla", null, true, Instant.now(), Instant.now());
                ServiceResponse service = new ServiceResponse(
                                serviceId, businessId, UUID.randomUUID(), "hair", "Cabeleireiro & Tranças",
                                "Corte & Brushing", "Descrição", 60,
                                new BigDecimal("2500.00"), true, List.of(), Instant.now(), Instant.now());

                when(userAccountService.existsById(customerId)).thenReturn(true);
                when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
                when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId))
                                .thenReturn(Optional.of(employee));
                when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId))
                                .thenReturn(Optional.of(service));
                when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(true);
                when(businessEmployeeQueryService.isServiceAssignedToEmployee(businessId, employeeId, serviceId))
                                .thenReturn(false);

                // Act & Assert
                assertThatThrownBy(() -> createAppointmentUseCase.execute(customerId, request))
                                .isInstanceOf(ValidationException.class)
                                .hasMessage("O profissional selecionado não realiza este serviço.");
                verify(appointmentRepository, never()).saveAndFlush(any());
        }
}
