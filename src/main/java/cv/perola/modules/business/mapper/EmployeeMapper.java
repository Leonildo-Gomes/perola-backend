package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.BusinessEmployeeResponse;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.identity.service.UserSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EmployeeMapper {

    public EmployeeEntity toEntity(UUID businessId, UUID userId, String displayName) {
        return EmployeeEntity.builder()
                .businessId(businessId)
                .userId(userId)
                .displayName(displayName != null ? displayName.trim() : null)
                .isActive(true)
                .build();
    }

    public EmployeeEntity toEntity(CreateEmployeeRequest request, UUID businessId) {
        return EmployeeEntity.builder()
                .businessId(businessId)
                .userId(request.userId())
                .displayName(request.displayName() != null ? request.displayName().trim() : null)
                .specialty(normalizeSpecialty(request.specialty()))
                .isActive(true)
                .build();
    }

    /**
     * Normaliza o campo opcional de especialidade removendo espaços em excesso
     * e convertendo valores em branco para {@code null}.
     */
    public String normalizeSpecialty(String specialty) {
        if (specialty == null) {
            return null;
        }
        String trimmed = specialty.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public EmployeeResponse toResponse(EmployeeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EmployeeResponse(
                entity.getId(),
                entity.getBusinessId(),
                entity.getUserId(),
                entity.getDisplayName(),
                entity.getSpecialty(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BusinessEmployeeResponse toBusinessResponse(
            EmployeeEntity entity,
            UserSummary userSummary,
            String accessRole,
            List<UUID> assignedServiceIds) {
        if (entity == null) {
            return null;
        }
        List<UUID> safeAssignedServiceIds = assignedServiceIds != null ? List.copyOf(assignedServiceIds) : List.of();
        return new BusinessEmployeeResponse(
                entity.getId(),
                entity.getBusinessId(),
                entity.getUserId(),
                entity.getDisplayName(),
                userSummary != null ? userSummary.email() : null,
                userSummary != null ? userSummary.phone() : null,
                userSummary != null ? userSummary.avatarUrl() : null,
                accessRole,
                entity.getSpecialty(),
                safeAssignedServiceIds,
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

