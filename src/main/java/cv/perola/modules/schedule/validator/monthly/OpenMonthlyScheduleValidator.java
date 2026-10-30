package cv.perola.modules.schedule.validator.monthly;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.dto.HolidayDecisionRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class OpenMonthlyScheduleValidator {

    public void validatePermissions(EmployeeResponse employee, UserPrincipal principal, UUID businessId) {
        boolean isOwner = principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));

        if (isOwner) {
            return;
        }

        boolean isOwnEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isOwnEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode abrir a sua própria agenda mensal.");
        }
    }

    public void validateScheduleActive(EmployeeScheduleEntity schedule) {
        if (!schedule.isActive()) {
            throw new ValidationException("Não é possível abrir a agenda mensal com um perfil de horário inativo.");
        }
    }

    public void validateHolidayDecisions(int year, int month, List<HolidayDecisionRequest> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return;
        }

        Set<LocalDate> seenDates = new HashSet<>();
        for (HolidayDecisionRequest decision : decisions) {
            LocalDate date = decision.holidayDate();
            if (!seenDates.add(date)) {
                throw new ValidationException("A data de feriado " + date + " foi informada mais de uma vez.");
            }
            if (date.getYear() != year || date.getMonthValue() != month) {
                throw new ValidationException("A data de feriado " + date
                        + " não pertence ao período de abertura (" + month + "/" + year + ").");
            }
        }
    }
}
