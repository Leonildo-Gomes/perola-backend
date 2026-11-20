package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.config.AppConfig;
import cv.perola.core.exception.PendingAppointmentsLimitExceededException;
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
import cv.perola.modules.booking.validator.appointment.CreateAppointmentValidator.AppointmentValidationContext;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.service.CustomerResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para criação transacional e atómica de um agendamento (BE-013, BE-014, BE-022),
 * com suporte a Guest Booking público e agendamento presencial por colaboradores (walk-in),
 * validação anti-spam de marcações pendentes, auto-registo CRM em business_customers,
 * blindado pela constraint física GiST ex_appointments_no_overlap no PostgreSQL,
 * com registo na trilha de auditoria append-only e publicação de evento de domínio.
 */
@Service
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final BusinessCustomerRepository businessCustomerRepository;
    private final CustomerResolutionService customerResolutionService;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CreateAppointmentValidator createAppointmentValidator;

    @Transactional
    public AppointmentResponse execute(UserPrincipal principal, CreateAppointmentRequest request) {
        CustomerEntity customer;
        UUID createdById;

        if (principal == null) {
            // Visitante sem autenticação (Guest Booking público)
            createAppointmentValidator.validateGuestContactDetails(request);
            customer = customerResolutionService.resolveForBooking(
                    request.customerName(),
                    request.customerEmail(),
                    request.customerPhone(),
                    null
            );
            createdById = null;
        } else if (request.customerEmail() != null && !request.customerEmail().isBlank()) {
            // Utilizador autenticado (Staff/Proprietário) a criar agendamento presencial/telefone para um cliente
            createAppointmentValidator.validateGuestContactDetails(request);
            customer = customerResolutionService.resolveForBooking(
                    request.customerName(),
                    request.customerEmail(),
                    request.customerPhone(),
                    null
            );
            createdById = principal.getId();
        } else {
            // Utilizador autenticado a agendar para si próprio
            customer = customerResolutionService.resolveForAuthenticatedUser(principal.getId());
            createdById = principal.getId();
        }

        // Anti-spam check: se o cliente já possui >= 3 marcações ativas (PENDING ou CONFIRMED) neste salão
        long activeCount = appointmentRepository.countByBusinessIdAndCustomerIdAndStatusIn(
                request.businessId(),
                customer.getId(),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
        );
        if (activeCount >= 3) {
            throw new PendingAppointmentsLimitExceededException(
                    "O cliente já possui o limite máximo de 3 marcações ativas (pendentes ou confirmadas) neste salão."
            );
        }

        AppointmentValidationContext context = createAppointmentValidator.validate(request);
        return createAppointmentInternal(customer, createdById, request, context);
    }

    @Transactional
    public AppointmentResponse execute(UUID customerId, CreateAppointmentRequest request) {
        CustomerEntity customer = customerResolutionService.findById(customerId)
                .or(() -> customerResolutionService.findByUserId(customerId))
                .orElseGet(() -> customerResolutionService.resolveForBooking(
                        request.customerName() != null ? request.customerName() : "Cliente",
                        request.customerEmail() != null ? request.customerEmail() : customerId + "@perola.cv",
                        request.customerPhone() != null ? request.customerPhone() : "+2389999999",
                        customerId
                ));

        AppointmentValidationContext context = createAppointmentValidator.validate(customerId, request);

        long activeCount = appointmentRepository.countByBusinessIdAndCustomerIdAndStatusIn(
                request.businessId(),
                customer.getId(),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
        );
        if (activeCount >= 3) {
            throw new PendingAppointmentsLimitExceededException(
                    "O cliente já possui o limite máximo de 3 marcações ativas (pendentes ou confirmadas) neste salão."
            );
        }

        return createAppointmentInternal(customer, customerId, request, context);
    }

    private AppointmentResponse createAppointmentInternal(
            CustomerEntity customer,
            UUID createdById,
            CreateAppointmentRequest request,
            AppointmentValidationContext context
    ) {
        BusinessResponse business = context.business();
        EmployeeResponse employee = context.employee();
        ServiceResponse service = context.service();

        // Auto-registo em business_customers com tier = NEW se não existir relacionamento prévio
        if (!businessCustomerRepository.existsByBusinessIdAndCustomerId(request.businessId(), customer.getId())) {
            BusinessCustomerEntity businessCustomer = BusinessCustomerEntity.builder()
                    .businessId(request.businessId())
                    .customerId(customer.getId())
                    .tier(CustomerTier.NEW)
                    .build();
            businessCustomerRepository.save(businessCustomer);
        }

        Instant startTime = request.startTime().toInstant();
        Instant endTime = startTime.plus(Duration.ofMinutes(service.durationMinutes()));

        AppointmentEntity appointment = appointmentMapper.toEntity(
                request,
                customer.getId(),
                createdById,
                service,
                startTime,
                endTime
        );

        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        AppointmentHistoryEntity historyEntry = appointmentMapper.toHistoryEntity(
                saved.getId(),
                createdById,
                AppointmentAction.CREATED,
                null,
                AppointmentStatus.PENDING,
                null,
                null,
                startTime,
                endTime,
                request.notes()
        );
        AppointmentHistoryEntity savedHistory = appointmentHistoryRepository.save(historyEntry);

        eventPublisher.publishEvent(new AppointmentLifecycleEvent(
                saved.getId(),
                saved.getBusinessId(),
                saved.getCustomerId(),
                saved.getEmployeeId(),
                createdById,
                AppointmentAction.CREATED.name(),
                null,
                AppointmentStatus.PENDING.name(),
                startTime,
                endTime,
                request.notes(),
                Instant.now()
        ));

        ZoneId businessZone = resolveZoneId(business.timezone());
        List<AppointmentHistoryEntity> historyList = savedHistory != null ? List.of(savedHistory) : List.of();

        return appointmentMapper.toResponse(
                saved,
                historyList,
                businessZone,
                business.name(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                employee.displayName(),
                null,
                null
        );
    }

    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ignored) {
            return ZoneId.of(AppConfig.CAPE_VERDE_TIMEZONE);
        }
    }
}
