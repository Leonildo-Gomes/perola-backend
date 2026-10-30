package cv.perola.modules.schedule.validator.monthly;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.HolidayDecisionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenMonthlyScheduleValidatorTest {

    private final OpenMonthlyScheduleValidator validator = new OpenMonthlyScheduleValidator();

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
    @DisplayName("Deve lançar TenantAccessDeniedException quando outro colaborador tentar abrir a agenda")
    void shouldThrowWhenOtherEmployeeTriesToOpen() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeResponse employee = new EmployeeResponse(UUID.randomUUID(), businessId, UUID.randomUUID(), "Outro", true, Instant.now(), Instant.now());
        UserPrincipal principal = new UserPrincipal(userId, "emp@perola.cv", "hash", List.of(UserRole.EMPLOYEE.name()), List.of(new TenantMembership(businessId, UserRole.EMPLOYEE.name())), true);

        assertThrows(TenantAccessDeniedException.class, () -> validator.validatePermissions(employee, principal, businessId));
    }

    @Test
    @DisplayName("Deve lançar ValidationException se o perfil de horário estiver inativo")
    void shouldThrowWhenScheduleIsInactive() {
        EmployeeScheduleEntity schedule = EmployeeScheduleEntity.builder()
                .isActive(false)
                .build();

        assertThrows(ValidationException.class, () -> validator.validateScheduleActive(schedule));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando feriado estiver duplicado ou fora do mês")
    void shouldThrowWhenHolidayIsDuplicateOrOutOfMonth() {
        List<HolidayDecisionRequest> decisions = List.of(
                new HolidayDecisionRequest(LocalDate.of(2026, 12, 25), true)
        );

        assertThrows(ValidationException.class, () -> validator.validateHolidayDecisions(2026, 11, decisions));
    }
}
