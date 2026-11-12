package cv.perola.modules.business.dto;

import cv.perola.modules.business.domain.enums.MediaScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Metadados e URL pública do ficheiro de imagem carregado")
public record MediaUploadResponse(
        @Schema(description = "Identificador único do registo de media", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID id,

        @Schema(description = "Escopo funcional do ficheiro", example = "SERVICE_IMAGE")
        MediaScope scope,

        @Schema(description = "Caminho relativo único no armazenamento", example = "businesses/3fa85f64-5717-4562-b3fc-2c963f66afa6/services/f47ac10b-58cc-4372-a567-0e02b2c3d479.webp")
        String storageKey,

        @Schema(description = "URL pública para leitura cacheada da imagem", example = "http://localhost:8080/api/v1/media/businesses/3fa85f64-5717-4562-b3fc-2c963f66afa6/services/f47ac10b-58cc-4372-a567-0e02b2c3d479.webp")
        String url,

        @Schema(description = "MIME type real detetado por Magic Bytes", example = "image/webp")
        String contentType,

        @Schema(description = "Tamanho do ficheiro em bytes", example = "84320")
        long sizeBytes,

        @Schema(description = "Data e hora do upload em UTC", example = "2026-11-12T12:30:00Z")
        Instant createdAt
) {
}

