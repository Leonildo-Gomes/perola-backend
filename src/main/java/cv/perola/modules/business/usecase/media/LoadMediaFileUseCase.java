package cv.perola.modules.business.usecase.media;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.storage.FileStoragePort;
import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.dto.MediaResourceResult;
import cv.perola.modules.business.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoadMediaFileUseCase {

    private final FileStoragePort fileStoragePort;
    private final MediaFileRepository mediaFileRepository;

    @Transactional(readOnly = true)
    public MediaResourceResult execute(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ValidationException("A chave de armazenamento (storageKey) é obrigatória.");
        }
        String normalizedKey = storageKey.trim();
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }
        if (normalizedKey.isBlank()) {
            throw new ValidationException("A chave de armazenamento (storageKey) é obrigatória.");
        }

        Resource resource = fileStoragePort.loadAsResource(normalizedKey);
        String finalKey = normalizedKey;
        String contentType = mediaFileRepository.findByStorageKey(finalKey)
                .map(MediaFileEntity::getContentType)
                .orElseGet(() -> inferContentTypeFromExtension(finalKey));

        return new MediaResourceResult(resource, contentType);
    }

    private String inferContentTypeFromExtension(String storageKey) {
        String lower = storageKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}

