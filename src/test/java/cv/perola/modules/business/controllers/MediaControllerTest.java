package cv.perola.modules.business.controllers;

import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.MediaResourceResult;
import cv.perola.modules.business.dto.MediaUploadResponse;
import cv.perola.modules.business.usecase.media.LoadMediaFileUseCase;
import cv.perola.modules.business.usecase.media.UploadMediaFileUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private UploadMediaFileUseCase uploadMediaFileUseCase;

    @Mock
    private LoadMediaFileUseCase loadMediaFileUseCase;

    @InjectMocks
    private MediaController mediaController;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve devolver 201 Created ao fazer upload de ficheiro de imagem válido")
    void shouldReturnCreatedOnUploadMedia() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        TenantContext.setTenantId(businessId);
        UserPrincipal principal = new UserPrincipal(userId, "owner@perola.cv", "hash", List.of("OWNER"), List.of(),
                true);
        MockMultipartFile file = new MockMultipartFile("file", "logo.webp", "image/webp", new byte[] { 1, 2, 3 });
        MediaUploadResponse expected = new MediaUploadResponse(
                UUID.randomUUID(),
                MediaScope.BUSINESS_LOGO,
                "businesses/" + businessId + "/logo/abc.webp",
                "http://localhost:8080/api/v1/media/businesses/" + businessId + "/logo/abc.webp",
                "image/webp",
                3L,
                Instant.now());
        when(uploadMediaFileUseCase.execute(principal, businessId, MediaScope.BUSINESS_LOGO, file))
                .thenReturn(expected);

        // Act
        ResponseEntity<MediaUploadResponse> response = mediaController.uploadMedia(principal, file,
                MediaScope.BUSINESS_LOGO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Deve servir ficheiro de imagem com Cache-Control imutável e Content-Type adequado")
    void shouldServeMediaFileWithImmutableCacheControlHeader() {
        // Arrange
        String storageKey = "/businesses/123/services/photo.webp";
        Resource resource = new ByteArrayResource(new byte[] { 10, 20, 30 });
        when(loadMediaFileUseCase.execute(storageKey))
                .thenReturn(new MediaResourceResult(resource, "image/webp"));

        // Act
        ResponseEntity<Resource> response = mediaController.getMediaFile(storageKey);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("image/webp"));
        assertThat(response.getBody()).isEqualTo(resource);
    }
}