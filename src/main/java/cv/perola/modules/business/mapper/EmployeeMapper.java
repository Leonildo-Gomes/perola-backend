package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.EmployeeEntity;
import org.springframework.stereotype.Component;

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
}
