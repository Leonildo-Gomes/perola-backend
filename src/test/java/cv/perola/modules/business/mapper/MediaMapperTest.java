package cv.perola.modules.business.mapper;

import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.MediaUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MediaMapperTest {

    private MediaMapper mediaMapper;

    @BeforeEach
    void setUp() {
        mediaMapper = new MediaMapper();
    }

    @Test
    @DisplayName("Deve converter dados de upload para MediaFileEntity corretamente")
    void shouldMapToEntityCorrectly() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID uploadedById = UUID.randomUUID();
        String storageKey = "businesses/" + businessId + "/services/file.webp";

        // Act
        MediaFileEntity entity = mediaMapper.toEntity(
                businessId,
                uploadedById,
                MediaScope.SERVICE_IMAGE,
                storageKey,
                "corte.webp",
                "image/webp",
                84320L
        );

        // Assert
        assertThat(entity.getBusinessId()).isEqualTo(businessId);
        assertThat(entity.getUploadedById()).isEqualTo(uploadedById);
        assertThat(entity.getScope()).isEqualTo(MediaScope.SERVICE_IMAGE);
        assertThat(entity.getStorageKey()).isEqualTo(storageKey);
        assertThat(entity.getOriginalFilename()).isEqualTo("corte.webp");
        assertThat(entity.getContentType()).isEqualTo("image/webp");
        assertThat(entity.getSizeBytes()).isEqualTo(84320L);
    }

    @Test
    @DisplayName("Deve converter MediaFileEntity para MediaUploadResponse corretamente")
    void shouldMapToResponseCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-12T12:30:00Z");
        MediaFileEntity entity = MediaFileEntity.builder()
                .id(id)
                .scope(MediaScope.BUSINESS_LOGO)
                .storageKey("businesses/123/logo/abc.png")
                .contentType("image/png")
                .sizeBytes(42000L)
                .createdAt(now)
                .build();
        String publicUrl = "http://localhost:8080/api/v1/media/businesses/123/logo/abc.png";

        // Act
        MediaUploadResponse response = mediaMapper.toResponse(entity, publicUrl);

        // Assert
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.scope()).isEqualTo(MediaScope.BUSINESS_LOGO);
        assertThat(response.storageKey()).isEqualTo("businesses/123/logo/abc.png");
        assertThat(response.url()).isEqualTo(publicUrl);
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.sizeBytes()).isEqualTo(42000L);
        assertThat(response.createdAt()).isEqualTo(now);
    }
}

