package cv.perola.modules.business.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.domain.enums.MediaScope;
import cv.perola.modules.business.dto.MediaResourceResult;
import cv.perola.modules.business.dto.MediaUploadResponse;
import cv.perola.modules.business.usecase.media.LoadMediaFileUseCase;
import cv.perola.modules.business.usecase.media.UploadMediaFileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/media")
@RequiredArgsConstructor
@Tag(name = "Media & Files", description = "Endpoint único de upload de imagens e leitura com cache imutável")
public class MediaController {

    public static final String IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final UploadMediaFileUseCase uploadMediaFileUseCase;
    private final LoadMediaFileUseCase loadMediaFileUseCase;

    @PostMapping(value = "/upload", version = "v1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Endpoint único de upload de ficheiro de imagem",
            description = "Recebe um ficheiro de imagem (multipart/form-data), valida Magic Bytes (image/jpeg, image/png, image/webp) e tamanho máximo (5 MB), grava no FileStoragePort, regista em media_files e devolve a URL pública. Para BUSINESS_LOGO, SERVICE_IMAGE e EMPLOYEE_AVATAR exige X-Business-Id com papel OWNER.",
            parameters = {
                    @Parameter(
                            name = TenantFilter.TENANT_HEADER,
                            in = ParameterIn.HEADER,
                            required = false,
                            description = "UUID do salão ativo (obrigatório para BUSINESS_LOGO, SERVICE_IMAGE e EMPLOYEE_AVATAR)",
                            schema = @Schema(type = "string", format = "uuid")
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagem carregada e registada com sucesso",
                    content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ficheiro inválido, formato não suportado ou tamanho > 5 MB",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso ao salão negado (X-Business-Id ausente ou utilizador não é OWNER)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam("scope") MediaScope scope
    ) {
        if (principal == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        UUID tenantBusinessId = TenantContext.findTenantId().orElse(null);
        MediaUploadResponse response = uploadMediaFileUseCase.execute(principal, tenantBusinessId, scope, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{*storageKey}", version = "v1")
    @Operation(
            summary = "Servir ficheiro de imagem estático pelo caminho relativo",
            description = "Devolve o binário da imagem com cabeçalho Cache-Control: public, max-age=31536000, immutable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Binário da imagem retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Caminho inválido"),
            @ApiResponse(responseCode = "404", description = "Ficheiro não encontrado")
    })
    public ResponseEntity<Resource> getMediaFile(
            @PathVariable("storageKey") String storageKey
    ) {
        MediaResourceResult result = loadMediaFileUseCase.execute(storageKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, IMMUTABLE_CACHE_CONTROL)
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.resource());
    }
}

