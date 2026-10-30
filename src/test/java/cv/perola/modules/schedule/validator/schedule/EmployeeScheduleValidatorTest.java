package cv.perola.modules.schedule.validator.schedule;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeScheduleValidatorTest {

    private final EmployeeScheduleValidator validator = new EmployeeScheduleValidator();

    private List<ScheduleDayItemRequest> buildStandardWeek() {
        List<ScheduleDayItemRequest> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                days.add(new ScheduleDayItemRequest(day, false, null, null, List.of()));
            } else {
                days.add(new ScheduleDayItemRequest(
                        day,
                        true,
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0),
                        List.of(new BreakItemRequest("Almoço", LocalTime.of(12, 0), LocalTime.of(13, 0)))
                ));
            }
        }
        return days;
    }

    @Test
    @DisplayName("Deve validar permissões com sucesso para o proprietário do salão")
    void shouldValidatePermissionsForOwner() {
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(
                UUID.randomUUID(), businessId, UUID.randomUUID(), "Func", true, java.time.Instant.now(), java.time.Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                ownerId, "owner@perola.cv", "hash",
                List.of(UserRole.OWNER.name()), List.of(new TenantMembership(businessId, UserRole.OWNER.name())), true
        );

        assertDoesNotThrow(() -> validator.validatePermissions(employee, principal, businessId));
    }

    @Test
    @DisplayName("Deve lançar TenantAccessDeniedException quando outro colaborador tentar editar o expediente")
    void shouldThrowWhenOtherEmployeeTriesToEdit() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(
                UUID.randomUUID(), businessId, UUID.randomUUID(), "Outro Func", true, java.time.Instant.now(), java.time.Instant.now()
        );
        UserPrincipal principal = new UserPrincipal(
                userId, "emp@perola.cv", "hash",
                List.of(UserRole.EMPLOYEE.name()), List.of(new TenantMembership(businessId, UserRole.EMPLOYEE.name())), true
        );

        assertThrows(TenantAccessDeniedException.class, () -> validator.validatePermissions(employee, principal, businessId));
    }

    @Test
    @DisplayName("Deve validar com sucesso uma semana completa de 7 dias")
    void shouldValidateStandardWeekSuccessfully() {
        List<ScheduleDayItemRequest> days = buildStandardWeek();
        assertDoesNotThrow(() -> validator.validateScheduleDaysAndBreaks(days));
    }

    @Test
    @DisplayName("Deve lançar ValidationException se a semana não contiver exatamente 7 dias")
    void shouldThrowWhenDaysSizeIsNot7() {
        assertThrows(ValidationException.class, () -> validator.validateScheduleDaysAndBreaks(List.of()));
    }
}

