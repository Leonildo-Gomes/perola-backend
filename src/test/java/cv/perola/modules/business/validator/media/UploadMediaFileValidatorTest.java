package cv.perola.modules.business.validator.media;

import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantMembership;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.validator.media.UploadMediaFileValidator.ValidatedMediaUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadMediaFileValidatorTest {

        private static final byte[] VALID_JPEG_BYTES = new byte[] {
                        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01
        };

        private static final byte[] VALID_PNG_BYTES = new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
        };

        private static final byte[] VALID_WEBP_BYTES = new byte[] {
                        'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };

        @Mock
        private BusinessRepository businessRepository;

        @InjectMocks
        private UploadMediaFileValidator validator;

        @Test
        @DisplayName("Deve validar ficheiro WebP autêntico para SERVICE_IMAGE quando utilizador é OWNER")
        void shouldValidateAuthenticWebpForServiceImageWhenOwner() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UserPrincipal principal = new UserPrincipal(
                                userId,
                                "owner@perola.cv",
                                "hash",
                                List.of("OWNER"),
                                List.of(new TenantMembership(businessId, "OWNER")),
                                true);
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "../../corte.webp",
                                "image/webp",
                                VALID_WEBP_BYTES);

                // Act
                ValidatedMediaUpload result = validator.validate(principal, businessId, MediaScope.SERVICE_IMAGE, file);

                // Assert
                assertThat(result.businessId()).isEqualTo(businessId);
                assertThat(result.uploadedById()).isEqualTo(userId);
                assertThat(result.scope()).isEqualTo(MediaScope.SERVICE_IMAGE);
                assertThat(result.contentType()).isEqualTo("image/webp");
                assertThat(result.extension()).isEqualTo("webp");
                assertThat(result.originalFilename()).isEqualTo("corte.webp");
        }

        @Test
        @DisplayName("Deve validar ficheiro JPEG e PNG autênticos para USER_AVATAR sem exigir X-Business-Id")
        void shouldValidateAuthenticJpegAndPngForUserAvatarWithoutTenantHeader() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UserPrincipal principal = new UserPrincipal(
                                userId,
                                "customer@perola.cv",
                                "hash",
                                List.of("CUSTOMER"),
                                List.of(),
                                true);
                MockMultipartFile jpegFile = new MockMultipartFile("file", "avatar.jpg", "image/jpeg",
                                VALID_JPEG_BYTES);
                MockMultipartFile pngFile = new MockMultipartFile("file", "avatar.png", "image/png", VALID_PNG_BYTES);

                // Act
                ValidatedMediaUpload jpegResult = validator.validate(principal, null, MediaScope.USER_AVATAR, jpegFile);
                ValidatedMediaUpload pngResult = validator.validate(principal, null, MediaScope.USER_AVATAR, pngFile);

                // Assert
                assertThat(jpegResult.businessId()).isNull();
                assertThat(jpegResult.contentType()).isEqualTo("image/jpeg");
                assertThat(jpegResult.extension()).isEqualTo("jpg");

                assertThat(pngResult.businessId()).isNull();
                assertThat(pngResult.contentType()).isEqualTo("image/png");
                assertThat(pngResult.extension()).isEqualTo("png");
        }

        @Test
        @DisplayName("Deve rejeitar script/executável disfarçado de .jpg com ValidationException (400 Bad Request)")
        void shouldRejectDisguisedExecutableOrScriptAsBadRequest() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UserPrincipal principal = new UserPrincipal(
                                userId,
                                "customer@perola.cv",
                                "hash",
                                List.of("CUSTOMER"),
                                List.of(),
                                true);
                byte[] maliciousBytes = "#!/bin/sh\nrm -rf /".getBytes(StandardCharsets.UTF_8);
                MockMultipartFile disguisedFile = new MockMultipartFile(
                                "file",
                                "malicious.jpg",
                                "image/jpeg",
                                maliciousBytes);

                // Act & Assert
                assertThatThrownBy(() -> validator.validate(principal, null, MediaScope.USER_AVATAR, disguisedFile))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("Formato de imagem inválido");
        }

        @Test
        @DisplayName("Deve rejeitar ficheiro com tamanho superior a 5 MB (5242880 bytes) com ValidationException")
        void shouldRejectFileLargerThanFiveMegabytes() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UserPrincipal principal = new UserPrincipal(
                                userId,
                                "customer@perola.cv",
                                "hash",
                                List.of("CUSTOMER"),
                                List.of(),
                                true);
                byte[] oversizedBytes = new byte[(int) UploadMediaFileValidator.MAX_FILE_SIZE_BYTES + 1];
                System.arraycopy(VALID_JPEG_BYTES, 0, oversizedBytes, 0, VALID_JPEG_BYTES.length);
                MockMultipartFile oversizedFile = new MockMultipartFile(
                                "file",
                                "huge.jpg",
                                "image/jpeg",
                                oversizedBytes);

                // Act & Assert
                assertThatThrownBy(() -> validator.validate(principal, null, MediaScope.USER_AVATAR, oversizedFile))
                                .isInstanceOf(ValidationException.class)
                                .hasMessageContaining("5 MB");
        }

        @ParameterizedTest
        @EnumSource(value = MediaScope.class, names = { "BUSINESS_LOGO", "SERVICE_IMAGE", "EMPLOYEE_AVATAR" })
        @DisplayName("Deve rejeitar escopos de salão sem X-Business-Id ou quando utilizador não é OWNER com TenantAccessDeniedException (403)")
        void shouldRejectBusinessScopesWithoutValidOwnerTenant(MediaScope scope) {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID businessId = UUID.randomUUID();
                UserPrincipal employeeOnlyPrincipal = new UserPrincipal(
                                userId,
                                "staff@perola.cv",
                                "hash",
                                List.of("EMPLOYEE"),
                                List.of(new TenantMembership(businessId, "EMPLOYEE")),
                                true);
                MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", VALID_WEBP_BYTES);
                when(businessRepository.findOwnerIdById(businessId)).thenReturn(Optional.of(UUID.randomUUID()));

                // Act & Assert 1: Sem X-Business-Id
                assertThatThrownBy(() -> validator.validate(employeeOnlyPrincipal, null, scope, file))
                                .isInstanceOf(TenantAccessDeniedException.class);

                // Act & Assert 2: Com X-Business-Id mas apenas EMPLOYEE (não OWNER)
                assertThatThrownBy(() -> validator.validate(employeeOnlyPrincipal, businessId, scope, file))
                                .isInstanceOf(TenantAccessDeniedException.class);
        }
}