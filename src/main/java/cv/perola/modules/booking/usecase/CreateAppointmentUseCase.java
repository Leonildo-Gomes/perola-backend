package cv.perola.modules.booking.usecase;

import cv.perola.core.config.AppConfig;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.shared.event.AppointmentLifecycleEvent;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.AppointmentHistoryEntity;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentHistoryRepository;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.service.UserAccountService;
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
 * Caso de uso para criação transacional e atómica de um agendamento (BE-013 / BE-014),
 * blindado pela constraint física GiST ex_appointments_no_overlap no PostgreSQL,
 * com registo na trilha de auditoria append-only e publicação de evento de domínio.
 */
@Service
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final UserAccountService userAccountService;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AppointmentResponse execute(UUID customerId, CreateAppointmentRequest request) {
        if (customerId == null || !userAccountService.existsById(customerId)) {
            throw new ResourceNotFoundException("Cliente não encontrado.");
        }

        BusinessResponse business = businessEmployeeQueryService.findBusinessById(request.businessId())
                .orElseThrow(() -> new ResourceNotFoundException("Salão não encontrado."));
        if (!business.isActive()) {
            throw new ValidationException("O salão encontra-se inativo para novos agendamentos.");
        }

        EmployeeResponse employee = businessEmployeeQueryService
                .findEmployeeByIdAndBusinessId(request.employeeId(), request.businessId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado para o salão especificado."));
        if (!employee.isActive()) {
            throw new ValidationException("O profissional selecionado encontra-se inativo.");
        }

        ServiceResponse service = businessEmployeeQueryService
                .findServiceByIdAndBusinessId(request.serviceId(), request.businessId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado para o salão especificado."));
        if (!service.isActive()) {
            throw new ValidationException("O serviço selecionado encontra-se inativo.");
        }

        if (businessEmployeeQueryService.hasAnyAssignedServices(request.businessId(), request.employeeId())
                && !businessEmployeeQueryService.isServiceAssignedToEmployee(
                        request.businessId(), request.employeeId(), request.serviceId())) {
            throw new ValidationException("O profissional selecionado não realiza este serviço.");
        }

        Instant startTime = request.startTime().toInstant();
        Instant endTime = startTime.plus(Duration.ofMinutes(service.durationMinutes()));

        AppointmentEntity appointment = appointmentMapper.toEntity(
                request,
                customerId,
                customerId,
                service,
                startTime,
                endTime
        );

        AppointmentEntity saved = appointmentRepository.saveAndFlush(appointment);

        AppointmentHistoryEntity historyEntry = appointmentMapper.toHistoryEntity(
                saved.getId(),
                customerId,
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
                customerId,
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

        return appointmentMapper.toResponse(saved, historyList, businessZone);
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
