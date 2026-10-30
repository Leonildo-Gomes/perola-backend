package cv.perola.modules.schedule.validator.timeoff;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateTimeOffValidator {

    public void validatePermissions(EmployeeResponse employee, UserPrincipal principal, UUID businessId) {
        boolean isOwner = principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));

        if (isOwner) {
            return;
        }

        boolean isOwnEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isOwnEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode registar ausências para o seu próprio perfil.");
        }
    }

    public void validateDatesAndTimes(CreateTimeOffRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ValidationException("A data de término (" + request.endDate()
                    + ") não pode ser anterior à data de início (" + request.startDate() + ").");
        }

        if (!request.resolvedIsAllDay()) {
            if (request.startTime() == null || request.endTime() == null) {
                throw new ValidationException("Os horários de início e término são obrigatórios para ausências de período parcial.");
            }
            if (!request.endTime().isAfter(request.startTime())) {
                throw new ValidationException("O horário de término (" + request.endTime()
                        + ") deve ser posterior ao horário de início (" + request.startTime() + ").");
            }
        }
    }
}

