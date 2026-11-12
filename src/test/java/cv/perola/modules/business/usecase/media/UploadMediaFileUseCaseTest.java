package cv.perola.modules.business.usecase.media;

import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.storage.FileStoragePort;
import cv.perola.modules.business.domain.MediaFileEntity;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.MediaUploadResponse;
import cv.perola.modules.business.mapper.MediaMapper;
import cv.perola.modules.business.repository.MediaFileRepository;
import cv.perola.modules.business.validator.media.UploadMediaFileValidator;
import cv.perola.modules.business.validator.media.UploadMediaFileValidator.ValidatedMediaUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadMediaFileUseCaseTest {

        @Mock
        private UploadMediaFileValidator uploadMediaFileValidator;

        @Mock
        private FileStoragePort fileStoragePort;

        @Mock
        private MediaFileRepository mediaFileRepository;

        @Mock
        private MediaMapper mediaMapper;

        @InjectMocks
        private UploadMediaFileUseCase uploadMediaFileUseCase;

        @Test
        @DisplayName("Deve gravar ficheiro no FileStoragePort, persistir metadados em media_files e devolver MediaUploadResponse")
        void shouldStoreFilePersistMetadataAndReturnResponse() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UUID mediaId = UUID.randomUUID();
                byte[] content = new byte[] { 'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P' };
                UserPrincipal principal = new UserPrincipal(
                                userId,
                                "owner@perola.cv",
                                "hash",
                                List.of("OWNER"),
                                List.of(new TenantMembership(businessId, "OWNER")),
                                true);
                MockMultipartFile file = new MockMultipartFile("file", "service.webp", "image/webp", content);

                ValidatedMediaUpload validated = new ValidatedMediaUpload(
                                businessId,
                                userId,
                                MediaScope.SERVICE_IMAGE,
                                content,
                                "image/webp",
                                "webp",
                                "service.webp");
                when(uploadMediaFileValidator.validate(principal, businessId, MediaScope.SERVICE_IMAGE, file))
                                .thenReturn(validated);

                MediaFileEntity entity = MediaFileEntity.builder()
                                .id(mediaId)
                                .businessId(businessId)
                                .uploadedById(userId)
                                .scope(MediaScope.SERVICE_IMAGE)
                                .storageKey("businesses/" + businessId + "/services/uuid.webp")
                                .contentType("image/webp")
                                .sizeBytes(content.length)
                                .createdAt(Instant.now())
                                .build();
                when(mediaMapper.toEntity(eq(businessId), eq(userId), eq(MediaScope.SERVICE_IMAGE), any(),
                                eq("service.webp"), eq("image/webp"), anyLong()))
                                .thenReturn(entity);
                when(mediaFileRepository.save(entity)).thenReturn(entity);

                MediaUploadResponse expectedResponse = new MediaUploadResponse(
                                mediaId,
                                MediaScope.SERVICE_IMAGE,
                                entity.getStorageKey(),
                                "/api/v1/media/" + entity.getStorageKey(),
                                "image/webp",
                                content.length,
                                entity.getCreatedAt());
                when(mediaMapper.toResponse(eq(entity), any())).thenReturn(expectedResponse);

                // Act
                MediaUploadResponse response = uploadMediaFileUseCase.execute(principal, businessId,
                                MediaScope.SERVICE_IMAGE, file);

                // Assert
                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                verify(fileStoragePort).store(any(InputStream.class), keyCaptor.capture(), eq("image/webp"));
                assertThat(keyCaptor.getValue())
                                .startsWith("businesses/" + businessId + "/services/")
                                .endsWith(".webp");
                verify(mediaFileRepository).save(entity);
                assertThat(response).isEqualTo(expectedResponse);
        }
}