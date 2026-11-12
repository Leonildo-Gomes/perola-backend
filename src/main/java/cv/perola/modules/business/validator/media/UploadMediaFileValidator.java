package cv.perola.modules.business.validator.media;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.identity.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UploadMediaFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 5_242_880L; // 5 MB

    private final BusinessRepository businessRepository;

    public ValidatedMediaUpload validate(
            UserPrincipal principal,
            UUID tenantBusinessId,
            MediaScope scope,
            MultipartFile file) {
        if (principal == null || principal.getId() == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        if (scope == null) {
            throw new ValidationException("O escopo do ficheiro (scope) é obrigatório.");
        }

        UUID effectiveBusinessId = null;
        if (scope.requiresBusinessTenant()) {
            if (tenantBusinessId == null) {
                throw new TenantAccessDeniedException(
                        "O cabeçalho X-Business-Id é obrigatório para o escopo " + scope + ".");
            }
            if (!isOwnerOfBusiness(principal, tenantBusinessId)) {
                throw new TenantAccessDeniedException(
                        "Apenas o proprietário (OWNER) do salão tem permissão para carregar imagens no escopo " + scope + ".");
            }
            effectiveBusinessId = tenantBusinessId;
        }

        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new ValidationException("O ficheiro de imagem é obrigatório e não pode estar vazio.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("O tamanho do ficheiro excede o limite máximo permitido de 5 MB (5242880 bytes).");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ValidationException("Não foi possível ler o conteúdo do ficheiro enviado.");
        }

        if (bytes.length == 0) {
            throw new ValidationException("O ficheiro de imagem não pode estar vazio.");
        }
        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("O tamanho do ficheiro excede o limite máximo permitido de 5 MB (5242880 bytes).");
        }

        DetectedImageFormat format = detectMagicBytes(bytes);
        String sanitizedFilename = sanitizeOriginalFilename(file.getOriginalFilename());

        return new ValidatedMediaUpload(
                effectiveBusinessId,
                principal.getId(),
                scope,
                bytes,
                format.contentType(),
                format.extension(),
                sanitizedFilename
        );
    }

    private boolean isOwnerOfBusiness(UserPrincipal principal, UUID businessId) {
        boolean hasOwnerMembershipInToken = principal.getMemberships() != null
                && principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId())
                        && UserRole.OWNER.name().equalsIgnoreCase(m.role()));
        if (hasOwnerMembershipInToken) {
            return true;
        }
        return businessRepository.findOwnerIdById(businessId)
                .map(ownerId -> ownerId.equals(principal.getId()))
                .orElse(false);
    }

    private DetectedImageFormat detectMagicBytes(byte[] bytes) {
        if (isPng(bytes)) {
            return new DetectedImageFormat("image/png", "png");
        }
        if (isJpeg(bytes)) {
            return new DetectedImageFormat("image/jpeg", "jpg");
        }
        if (isWebp(bytes)) {
            return new DetectedImageFormat("image/webp", "webp");
        }
        throw new ValidationException(
                "Formato de imagem inválido ou não suportado. Apenas ficheiros JPEG, PNG e WebP autênticos são permitidos.");
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 // 'P'
                && bytes[2] == 0x4E // 'N'
                && bytes[3] == 0x47 // 'G'
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String sanitized = originalFilename.replace('\\', '/');
        int lastSlash = sanitized.lastIndexOf('/');
        if (lastSlash >= 0) {
            sanitized = sanitized.substring(lastSlash + 1);
        }
        sanitized = sanitized.replace("\0", "").trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized.length() > 255 ? sanitized.substring(0, 255) : sanitized;
    }

    private record DetectedImageFormat(String contentType, String extension) {
    }

    public record ValidatedMediaUpload(
            UUID businessId,
            UUID uploadedById,
            MediaScope scope,
            byte[] content,
            String contentType,
            String extension,
            String originalFilename
    ) {
    }
}

