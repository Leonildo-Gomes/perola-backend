package cv.perola.modules.business.usecase.media;

import cv.perola.core.exception.ValidationException;
import cv.perola.core.storage.FileStoragePort;
import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.dto.MediaResourceResult;
import cv.perola.modules.business.repository.MediaFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadMediaFileUseCaseTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @InjectMocks
    private LoadMediaFileUseCase loadMediaFileUseCase;

    @Test
    @DisplayName("Deve normalizar barra inicial do storageKey e devolver Resource com contentType registado")
    void shouldNormalizeLeadingSlashAndReturnResourceWithStoredContentType() {
        // Arrange
        String rawKey = "/businesses/123/services/photo.webp";
        String normalizedKey = "businesses/123/services/photo.webp";
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        MediaFileEntity entity = MediaFileEntity.builder()
                .storageKey(normalizedKey)
                .contentType("image/webp")
                .build();

        when(fileStoragePort.loadAsResource(normalizedKey)).thenReturn(resource);
        when(mediaFileRepository.findByStorageKey(normalizedKey)).thenReturn(Optional.of(entity));

        // Act
        MediaResourceResult result = loadMediaFileUseCase.execute(rawKey);

        // Assert
        assertThat(result.resource()).isEqualTo(resource);
        assertThat(result.contentType()).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("Deve inferir contentType pela extensão quando metadado não existir na tabela media_files")
    void shouldInferContentTypeFromExtensionWhenMetadataNotPresent() {
        // Arrange
        String storageKey = "users/123/avatar/photo.png";
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(fileStoragePort.loadAsResource(storageKey)).thenReturn(resource);
        when(mediaFileRepository.findByStorageKey(storageKey)).thenReturn(Optional.empty());

        // Act
        MediaResourceResult result = loadMediaFileUseCase.execute(storageKey);

        // Assert
        assertThat(result.resource()).isEqualTo(resource);
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando storageKey for vazio ou em branco")
    void shouldThrowValidationExceptionWhenStorageKeyIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> loadMediaFileUseCase.execute("   "))
                .isInstanceOf(ValidationException.class);
    }
}

