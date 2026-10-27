package cv.perola.modules.business.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.usecase.CreateServiceUseCase;
import cv.perola.modules.business.usecase.DeactivateServiceUseCase;
import cv.perola.modules.business.usecase.ListServicesUseCase;
import cv.perola.modules.business.usecase.UpdateServiceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/services")
@RequiredArgsConstructor
@Tag(name = "Salon Management", description = "Gestão do catálogo de serviços do salão (Requer X-Business-Id)")
@SecurityRequirement(name = "BearerAuth")
public class BusinessServiceController {

    private final CreateServiceUseCase createServiceUseCase;
    private final ListServicesUseCase listServicesUseCase;
    private final UpdateServiceUseCase updateServiceUseCase;
    private final DeactivateServiceUseCase deactivateServiceUseCase;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar novo serviço no catálogo do salão", description = "Adiciona um novo serviço com preço e duração estimada. Apenas o proprietário pode criar serviços.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no corpo da requisição"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao salão"),
            @ApiResponse(responseCode = "409", description = "Já existe um serviço ativo com o mesmo nome")
    })
    public ResponseEntity<ServiceResponse> createService(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse response = createServiceUseCase.execute(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'EMPLOYEE')")
    @Operation(summary = "Listar catálogo de serviços ativos do salão", description = "Retorna todos os serviços ativos do salão.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de serviços do salão",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao salão")
    })
    public ResponseEntity<List<ServiceResponse>> listServices(@PathVariable UUID businessId) {
        List<ServiceResponse> services = listServicesUseCase.execute(businessId);
        return ResponseEntity.ok(services);
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Atualizar serviço existente (substituição integral)", description = "Atualiza os dados de um serviço existente.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateServiceRequest request) {
        ServiceResponse response = updateServiceUseCase.execute(businessId, serviceId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{serviceId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Atualizar serviço existente (parcial)", description = "Atualiza parcialmente os campos de um serviço existente.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    public ResponseEntity<ServiceResponse> patchService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateServiceRequest request) {
        ServiceResponse response = updateServiceUseCase.execute(businessId, serviceId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar serviço (soft delete)", description = "Marca o serviço como inativo (is_active = false) preservando integridade de agendamentos históricos.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<Void> deactivateService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId) {
        deactivateServiceUseCase.execute(businessId, serviceId);
        return ResponseEntity.noContent().build();
    }
}
