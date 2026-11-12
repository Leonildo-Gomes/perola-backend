package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.MediaUploadResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MediaMapper {

    public MediaFileEntity toEntity(
            UUID businessId,
            UUID uploadedById,
            MediaScope scope,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes) {
        return MediaFileEntity.builder()
                .businessId(businessId)
                .uploadedById(uploadedById)
                .scope(scope)
                .storageKey(storageKey)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
    }

    public MediaUploadResponse toResponse(MediaFileEntity entity, String publicUrl) {
        if (entity == null) {
            return null;
        }
        return new MediaUploadResponse(
                entity.getId(),
                entity.getScope(),
                entity.getStorageKey(),
                publicUrl,
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getCreatedAt()
        );
    }
}

