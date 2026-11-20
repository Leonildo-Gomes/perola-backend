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
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CreateAppointmentValidator {

    private static final Pattern CV_PHONE_PATTERN = Pattern.compile("^(\\+238)?[2359]\\d{6}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final BusinessEmployeeQueryService businessEmployeeQueryService;
    private final UserAccountService userAccountService;

    public record AppointmentValidationContext(
            BusinessResponse business,
            EmployeeResponse employee,
            ServiceResponse service
    ) {}

    public void validateGuestContactDetails(CreateAppointmentRequest request) {
        if (request.customerName() == null || request.customerName().trim().length() < 2) {
            throw new ValidationException("O nome do cliente é obrigatório e deve conter pelo menos 2 caracteres.");
        }
        if (request.customerEmail() == null || request.customerEmail().trim().isBlank()
                || !EMAIL_PATTERN.matcher(request.customerEmail().trim()).matches()) {
            throw new ValidationException("O email do cliente é obrigatório e deve ser válido.");
        }
        if (request.customerPhone() == null || request.customerPhone().trim().isBlank()
                || !CV_PHONE_PATTERN.matcher(request.customerPhone().trim()).matches()) {
            throw new ValidationException("O telefone do cliente deve ser um número válido de Cabo Verde (ex: +2389912345 ou 9912345).");
        }
    }

    public AppointmentValidationContext validate(CreateAppointmentRequest request) {
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

    public AppointmentValidationContext validate(UUID customerId, CreateAppointmentRequest request) {
        if (customerId != null && !userAccountService.existsById(customerId)) {
            throw new ResourceNotFoundException("Cliente não encontrado.");
        }
        return validate(request);
    }
}
