package cv.perola.core.storage;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implementação padrão de {@link FileStoragePort} em sistema de ficheiros local
 * / Docker Volume,
 * com proteção estrita contra Path Traversal via {@link Path#normalize()}.
 */
@Component
public class LocalFileSystemStorageAdapter implements FileStoragePort {

    private final Path rootLocation;

    public LocalFileSystemStorageAdapter(
            @Value("${app.storage.local-dir:./data/uploads}") String localDir) {
        if (localDir == null || localDir.isBlank()) {
            throw new IllegalArgumentException("O diretório raiz de armazenamento não pode ser nulo ou vazio.");
        }
        this.rootLocation = Paths.get(localDir.trim()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível inicializar o diretório de uploads: " + this.rootLocation,
                    ex);
        }
    }

    @Override
    public void store(InputStream inputStream, String storageKey, String contentType) {
        if (inputStream == null) {
            throw new ValidationException("O conteúdo do ficheiro não pode ser nulo.");
        }
        Path targetFile = resolveSafePath(storageKey);
        try {
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Erro ao gravar ficheiro em armazenamento local: " + storageKey, ex);
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        Path targetFile = resolveSafePath(storageKey);
        if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile) || !Files.isReadable(targetFile)) {
            throw new ResourceNotFoundException("Ficheiro de media não encontrado: " + storageKey);
        }
        return new FileSystemResource(targetFile);
    }

    @Override
    public void delete(String storageKey) {
        Path targetFile = resolveSafePath(storageKey);
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException ex) {
            throw new UncheckedIOException("Erro ao remover ficheiro de armazenamento local: " + storageKey, ex);
        }
    }

    private Path resolveSafePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ValidationException("A chave de armazenamento (storageKey) é obrigatória.");
        }
        String sanitized = storageKey.trim();
        if (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.isBlank()
                || sanitized.contains("..")
                || sanitized.contains("\\")
                || sanitized.contains("\0")) {
            throw new ValidationException("Caminho de ficheiro inválido ou não permitido.");
        }

        Path resolved = this.rootLocation.resolve(sanitized).normalize();
        if (!resolved.startsWith(this.rootLocation) || resolved.equals(this.rootLocation)) {
            throw new ValidationException("Caminho de ficheiro fora do diretório permitido.");
        }
        return resolved;
    }
}