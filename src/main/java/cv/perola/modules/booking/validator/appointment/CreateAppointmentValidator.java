package cv.perola.modules.booking.validator.appointment;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateAppointmentValidator {

    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final UserAccountService userAccountService;

    public record AppointmentValidationContext(
            BusinessResponse business,
            EmployeeResponse employee,
            ServiceResponse service
    ) {}

    public AppointmentValidationContext validate(UUID customerId, CreateAppointmentRequest request) {
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

        return new AppointmentValidationContext(business, employee, service);
    }
}

