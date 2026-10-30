package cv.perola.modules.booking.validator.appointment;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentValidatorTest {

    @Mock
    private BusinessEmployeeQueryService businessEmployeeQueryService;

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private CreateAppointmentValidator validator;

    private CreateAppointmentRequest buildSampleRequest(UUID businessId, UUID employeeId, UUID serviceId) {
        return new CreateAppointmentRequest(
                businessId,
                employeeId,
                serviceId,
                OffsetDateTime.parse("2026-11-03T10:00:00-01:00"),
                "Corte simples");
    }

    @Test
    @DisplayName("Deve validar com sucesso quando todos os dados e vínculos forem válidos")
    void shouldValidateSuccessfully() {
        UUID customerId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        CreateAppointmentRequest request = buildSampleRequest(businessId, employeeId, serviceId);

        BusinessResponse business = new BusinessResponse(businessId, UUID.randomUUID(), "Salão", "Santiago", "Praia",
                "Rua 1", "+23899", "Atlantic/Cape_Verde", true, Instant.now(), Instant.now());
        EmployeeResponse employee = new EmployeeResponse(employeeId, businessId, UUID.randomUUID(), "Func", true,
                Instant.now(), Instant.now());
        ServiceResponse service = new ServiceResponse(serviceId, businessId, "Corte", "Desc", 45,
                BigDecimal.valueOf(1000), true, Instant.now(), Instant.now());

        when(userAccountService.existsById(customerId)).thenReturn(true);
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(business));
        when(businessEmployeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId))
                .thenReturn(Optional.of(employee));
        when(businessEmployeeQueryService.findServiceByIdAndBusinessId(serviceId, businessId))
                .thenReturn(Optional.of(service));
        when(businessEmployeeQueryService.hasAnyAssignedServices(businessId, employeeId)).thenReturn(false);

        CreateAppointmentValidator.AppointmentValidationContext context = assertDoesNotThrow(
                () -> validator.validate(customerId, request));
        assertNotNull(context);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando cliente não existir")
    void shouldThrowWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        CreateAppointmentRequest request = buildSampleRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(userAccountService.existsById(customerId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> validator.validate(customerId, request));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o salão estiver inativo")
    void shouldThrowWhenBusinessIsInactive() {
        UUID customerId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        CreateAppointmentRequest request = buildSampleRequest(businessId, UUID.randomUUID(), UUID.randomUUID());

        BusinessResponse inactiveBusiness = new BusinessResponse(businessId, UUID.randomUUID(), "Salão Inativo",
                "Santiago", "Praia", "Rua 1", "+23899", "Atlantic/Cape_Verde", false, Instant.now(), Instant.now());

        when(userAccountService.existsById(customerId)).thenReturn(true);
        when(businessEmployeeQueryService.findBusinessById(businessId)).thenReturn(Optional.of(inactiveBusiness));

        assertThrows(ValidationException.class, () -> validator.validate(customerId, request));
    }
}
