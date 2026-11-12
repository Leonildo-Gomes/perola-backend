package cv.perola.core.storage;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileSystemStorageAdapterTest {

    @TempDir
    Path tempDir;

    private LocalFileSystemStorageAdapter storageAdapter;

    @BeforeEach
    void setUp() {
        storageAdapter = new LocalFileSystemStorageAdapter(tempDir.toString());
    }

    @Test
    @DisplayName("Deve gravar e carregar ficheiro como Resource no diretório local")
    void shouldStoreAndLoadFileAsResource() throws IOException {
        // Arrange
        byte[] content = "image-binary-content".getBytes(StandardCharsets.UTF_8);
        String storageKey = "businesses/3fa85f64/services/f47ac10b.webp";

        // Act
        storageAdapter.store(new ByteArrayInputStream(content), storageKey, "image/webp");
        Resource resource = storageAdapter.loadAsResource(storageKey);

        // Assert
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("Deve remover ficheiro existente com sucesso")
    void shouldDeleteExistingFile() {
        // Arrange
        byte[] content = "avatar-bytes".getBytes(StandardCharsets.UTF_8);
        String storageKey = "users/123e4567/avatar/a1b2c3d4.jpg";
        storageAdapter.store(new ByteArrayInputStream(content), storageKey, "image/jpeg");

        // Act
        storageAdapter.delete(storageKey);

        // Assert
        assertThatThrownBy(() -> storageAdapter.loadAsResource(storageKey))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../secret.txt",
            "businesses/../../etc/passwd",
            "businesses\\evil.webp",
            "   ",
            "/"
    })
    @DisplayName("Deve bloquear tentativas de Path Traversal ou chaves inválidas com ValidationException")
    void shouldRejectPathTraversalAndInvalidKeys(String maliciousKey) {
        // Arrange
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        // Act & Assert
        assertThatThrownBy(() -> storageAdapter.store(stream, maliciousKey, "image/webp"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao carregar ficheiro inexistente")
    void shouldThrowResourceNotFoundExceptionWhenFileDoesNotExist() {
        // Arrange
        String missingKey = "businesses/3fa85f64/services/non-existent.webp";

        // Act & Assert
        assertThatThrownBy(() -> storageAdapter.loadAsResource(missingKey))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

