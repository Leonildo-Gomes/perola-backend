package cv.perola.modules.booking.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.service.UserAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BusinessEmployeeQueryService businessEmployeeQueryService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private CreateAppointmentUseCase createAppointmentUseCase;

    @Test
    @DisplayName("Deve criar agendamento com sucesso, calcular end_time e persistir com saveAndFlush")
    void shouldCreateAppointmentSuccessfullyWithSnapshotAndCalculatedEndTime() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        OffsetDateTime startOffset = OffsetDateTime.of(2026, 11, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-1));
        Instant expectedStart = Instant.parse("2026-11-03T11:00:00Z");
        Instant expectedEnd = Instant.parse("2026-11-03T12:00:00Z");

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                startOffset,
                "Primeira visita"
        );

        BusinessResponse business = new BusinessResponse(
                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                "Platô", "+2382610000", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now()
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carla", true, Instant.now(), Instant.now()
        );
        ServiceResponse service = new ServiceResponse(
                serviceId, businessId, "Corte & Brushing", "Descrição", 60,
                new BigDecimal("2500.00"), true, Instant.now(), Instant.now()
        );

        AppointmentEntity mappedEntity = AppointmentEntity.builder()
                .businessId(businessId)
                .customerId(customerId)
                .employeeId(employeeId)
                .serviceId(serviceId)
                .createdById(customerId)
                .startTime(expectedStart)
                .endTime(expectedEnd)
                .serviceName("Corte & Brushing")
                .servicePrice(new BigDecimal("2500.00"))
                .serviceDuration(60)
                .status(AppointmentStatus.PENDING)
                .notes("Primeira visita")
                .build();
        mappedEntity.setId(appointmentId);

        AppointmentResponse expectedResponse = new AppointmentResponse(
                appointmentId,
                businessId,
                customerId,
                employeeId,
                serviceId,
                "Corte & Brushing",
                new BigDecimal("2500.00"),
                60,
                startOffset,
                startOffset.plusMinutes(60),
                null,
                null,
                AppointmentStatus.PENDING,
                "Primeira visita",
                startOffset
        );

        when(userAccountService.existsById(customerId)).thenReturn(true);
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
        when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(true);
        when(businessEmployeeQueryService.isServiceAssignedToEmployee(businessId, employeeId, serviceId)).thenReturn(true);
        when(appointmentMapper.toEntity(request, customerId, customerId, service, expectedStart, expectedEnd))
                .thenReturn(mappedEntity);
        when(appointmentRepository.saveAndFlush(mappedEntity)).thenReturn(mappedEntity);
        when(appointmentMapper.toResponse(mappedEntity, ZoneId.of("Atlantic/Cape_Verde"))).thenReturn(expectedResponse);

        // Act
        AppointmentResponse result = createAppointmentUseCase.execute(customerId, request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(appointmentRepository).saveAndFlush(mappedEntity);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o cliente não existir")
    void shouldThrowResourceNotFoundExceptionWhenCustomerDoesNotExist() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null
        );
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
                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null
        );
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
                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null
        );
        BusinessResponse business = new BusinessResponse(
                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                "Platô", "+2382610000", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now()
        );
        when(userAccountService.existsById(customerId)).thenReturn(true);
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.empty());

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
                OffsetDateTime.now(ZoneOffset.ofHours(-1)), null
        );
        BusinessResponse business = new BusinessResponse(
                businessId, UUID.randomUUID(), "Salão Pérola", "Santiago", "Praia",
                "Platô", "+2382610000", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now()
        );
        EmployeeResponse employee = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carla", true, Instant.now(), Instant.now()
        );
        ServiceResponse service = new ServiceResponse(
                serviceId, businessId, "Corte & Brushing", "Descrição", 60,
                new BigDecimal("2500.00"), true, Instant.now(), Instant.now()
        );

        when(userAccountService.existsById(customerId)).thenReturn(true);
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(employee));
        when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
        when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(true);
        when(businessEmployeeQueryService.isServiceAssignedToEmployee(businessId, employeeId, serviceId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> createAppointmentUseCase.execute(customerId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("O profissional selecionado não realiza este serviço.");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }
}

