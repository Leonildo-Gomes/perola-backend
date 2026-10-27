package cv.perola.modules.business.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.CreateBusinessRequest;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.usecase.CreateBusinessUseCase;
import cv.perola.modules.business.usecase.GetBusinessUseCase;
import cv.perola.modules.business.usecase.UpdateBusinessUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/businesses")
@Tag(name = "Salon Management", description = "Gestão do estabelecimento, serviços, equipa e configurações (Requer X-Business-Id)")
@RequiredArgsConstructor
public class BusinessController {

    private final CreateBusinessUseCase createBusinessUseCase;
    private final GetBusinessUseCase getBusinessUseCase;
    private final UpdateBusinessUseCase updateBusinessUseCase;

    @Operation(
            summary = "Registar novo salão (Onboarding de negócio)",
            description = "Regista o estabelecimento, atribui papéis de OWNER e EMPLOYEE ao utilizador e provisiona o perfil profissional inicial."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Salão criado com sucesso",
                    content = @Content(schema = @Schema(implementation = BusinessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação obrigatória",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Proprietário já possui um salão registado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(version = "v1")
    public ResponseEntity<BusinessResponse> createBusiness(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBusinessRequest request
    ) {
        if (principal == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        BusinessResponse response = createBusinessUseCase.execute(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Obter dados do próprio salão",
            description = "Retorna configurações e informações do salão. Requer o cabeçalho X-Business-Id correspondente.",
            parameters = {
                    @Parameter(
                            name = "X-Business-Id",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Identificador UUID do salão",
                            schema = @Schema(type = "string", format = "uuid")
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do salão retornados com sucesso",
                    content = @Content(schema = @Schema(implementation = BusinessResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este salão (X-Business-Id ausente ou inválido)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Salão não encontrado ou inativo",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/{id}", version = "v1")
    public ResponseEntity<BusinessResponse> getBusiness(
            @PathVariable("id") UUID id
    ) {
        BusinessResponse response = getBusinessUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Atualizar configurações do salão",
            description = "Atualiza nome, localização, telefone de contacto ou fuso horário. Requer o cabeçalho X-Business-Id correspondente.",
            parameters = {
                    @Parameter(
                            name = "X-Business-Id",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Identificador UUID do salão",
                            schema = @Schema(type = "string", format = "uuid")
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salão atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = BusinessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este salão (X-Business-Id ausente ou inválido)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Salão não encontrado ou inativo",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(value = "/{id}", version = "v1")
    public ResponseEntity<BusinessResponse> updateBusiness(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBusinessRequest request
    ) {
        BusinessResponse response = updateBusinessUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }
}
