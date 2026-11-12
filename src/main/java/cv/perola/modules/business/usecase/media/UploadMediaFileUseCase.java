package cv.perola.modules.business.usecase.media;

import cv.perola.core.security.UserPrincipal;
import cv.perola.core.storage.FileStoragePort;
import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.MediaUploadResponse;
import cv.perola.modules.business.mapper.MediaMapper;
import cv.perola.modules.business.repository.MediaFileRepository;
import cv.perola.modules.business.validator.media.UploadMediaFileValidator;
import cv.perola.modules.business.validator.media.UploadMediaFileValidator.ValidatedMediaUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadMediaFileUseCase {

    private final UploadMediaFileValidator uploadMediaFileValidator;
    private final FileStoragePort fileStoragePort;
    private final MediaFileRepository mediaFileRepository;
    private final MediaMapper mediaMapper;

    @Transactional
    public MediaUploadResponse execute(
            UserPrincipal principal,
            UUID tenantBusinessId,
            MediaScope scope,
            MultipartFile file) {
        ValidatedMediaUpload validated = uploadMediaFileValidator.validate(principal, tenantBusinessId, scope, file);

        UUID fileUuid = UUID.randomUUID();
        String storageKey = buildStorageKey(
                validated.scope(),
                validated.uploadedById(),
                validated.businessId(),
                fileUuid,
                validated.extension()
        );

        fileStoragePort.store(
                new ByteArrayInputStream(validated.content()),
                storageKey,
                validated.contentType()
        );

        MediaFileEntity entity = mediaMapper.toEntity(
                validated.businessId(),
                validated.uploadedById(),
                validated.scope(),
                storageKey,
                validated.originalFilename(),
                validated.contentType(),
                validated.content().length
        );
        MediaFileEntity saved = mediaFileRepository.save(entity);

        String publicUrl = buildPublicUrl(storageKey);
        return mediaMapper.toResponse(saved, publicUrl);
    }

    private String buildStorageKey(
            MediaScope scope,
            UUID userId,
            UUID businessId,
            UUID fileUuid,
            String extension) {
        return switch (scope) {
            case USER_AVATAR -> "users/" + userId + "/avatar/" + fileUuid + "." + extension;
            case BUSINESS_LOGO -> "businesses/" + businessId + "/logo/" + fileUuid + "." + extension;
            case SERVICE_IMAGE -> "businesses/" + businessId + "/services/" + fileUuid + "." + extension;
            case EMPLOYEE_AVATAR -> "businesses/" + businessId + "/employees/" + fileUuid + "." + extension;
        };
    }

    private String buildPublicUrl(String storageKey) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/media/")
                    .path(storageKey)
                    .toUriString();
        } catch (IllegalStateException ex) {
            return "/api/v1/media/" + storageKey;
        }
    }
}

