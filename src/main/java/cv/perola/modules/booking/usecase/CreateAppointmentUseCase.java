package cv.perola.modules.booking.usecase;

import cv.perola.core.config.AppConfig;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Caso de uso para criação transacional e atómica de um agendamento (BE-013),
 * blindado pela constraint física GiST ex_appointments_no_overlap no PostgreSQL.
 */
@Service
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final UserAccountService userAccountService;
    private final AppointmentMapper appointmentMapper;

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
        ZoneId businessZone = resolveZoneId(business.timezone());

        return appointmentMapper.toResponse(saved, businessZone);
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

