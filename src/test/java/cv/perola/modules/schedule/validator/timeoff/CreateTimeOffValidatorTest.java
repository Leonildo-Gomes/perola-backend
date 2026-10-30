package cv.perola.modules.schedule.validator.timeoff;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.enums.TimeOffType;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateTimeOffValidatorTest {

    private final CreateTimeOffValidator validator = new CreateTimeOffValidator();

    @Test
    @DisplayName("Deve validar permissões para o proprietário com sucesso")
    void shouldValidatePermissionsForOwner() {
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(UUID.randomUUID(), businessId, UUID.randomUUID(), "Func", true, Instant.now(), Instant.now());
        UserPrincipal principal = new UserPrincipal(ownerId, "owner@perola.cv", "hash", List.of(UserRole.OWNER.name()), List.of(new TenantMembership(businessId, UserRole.OWNER.name())), true);

        assertDoesNotThrow(() -> validator.validatePermissions(employee, principal, businessId));
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando outro colaborador tentar registar ausência")
    void shouldThrowWhenOtherEmployeeTriesToCreateTimeOff() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(UUID.randomUUID(), businessId, UUID.randomUUID(), "Outro", true, Instant.now(), Instant.now());
        UserPrincipal principal = new UserPrincipal(userId, "emp@perola.cv", "hash", List.of(UserRole.EMPLOYEE.name()), List.of(new TenantMembership(businessId, UserRole.EMPLOYEE.name())), true);

        assertThrows(TenantAccessDeniedException.class, () -> validator.validatePermissions(employee, principal, businessId));
    }

    @Test
    @DisplayName("Deve lançar ValidationException se data final for anterior à inicial")
    void shouldThrowWhenEndDateIsBeforeStartDate() {
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 5),
                true, null, null,
                TimeOffType.VACATION,
                "Férias"
        );

        assertThrows(ValidationException.class, () -> validator.validateDatesAndTimes(request));
    }

    @Test
    @DisplayName("Deve lançar ValidationException se ausência parcial não tiver horários válidos")
    void shouldThrowWhenPartialTimeOffHasInvalidTimes() {
        CreateTimeOffRequest request = new CreateTimeOffRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 10),
                false, LocalTime.of(12, 0), LocalTime.of(10, 0),
                TimeOffType.PERSONAL,
                "Médico"
        );

        assertThrows(ValidationException.class, () -> validator.validateDatesAndTimes(request));
    }
}

