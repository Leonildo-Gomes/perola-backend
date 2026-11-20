package cv.perola.modules.booking.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.BusinessCustomerDetailResponse;
import cv.perola.modules.booking.dto.BusinessCustomerResponse;
import cv.perola.modules.booking.dto.UpdateBusinessCustomerRequest;
import cv.perola.modules.booking.usecase.customer.GetBusinessCustomerDetailUseCase;
import cv.perola.modules.booking.usecase.customer.ListBusinessCustomersUseCase;
import cv.perola.modules.booking.usecase.customer.UpdateBusinessCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/businesses/{businessId}/customers")
@RequiredArgsConstructor
@Tag(name = "Salon Customers CRM", description = "Gestão da carteira de clientes, classificação VIP e notas internas privadas (Requer X-Business-Id)")
@SecurityRequirement(name = "BearerAuth")
public class BusinessCustomerController {

    private final ListBusinessCustomersUseCase listBusinessCustomersUseCase;
    private final GetBusinessCustomerDetailUseCase getBusinessCustomerDetailUseCase;
    private final UpdateBusinessCustomerUseCase updateBusinessCustomerUseCase;

    @GetMapping(version = "v1")
    @Operation(summary = "Listar clientes da carteira do salão", description = "Retorna a lista paginada de clientes com métricas de agendamentos, com suporte a filtros por busca textual e tier.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes obtida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ativo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<BusinessCustomerResponse>> listCustomers(
            @PathVariable UUID businessId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerTier tier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<BusinessCustomerResponse> response = listBusinessCustomersUseCase.execute(
                businessId, search, tier, page, size
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{customerId}", version = "v1")
    @Operation(summary = "Obter ficha de cliente do salão", description = "Retorna os dados detalhados da ficha do cliente no salão acompanhados do histórico recente de marcações.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha do cliente obtida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ativo", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ficha do cliente não encontrada para este salão", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<BusinessCustomerDetailResponse> getCustomerDetail(
            @PathVariable UUID businessId,
            @PathVariable UUID customerId
    ) {
        BusinessCustomerDetailResponse response = getBusinessCustomerDetailUseCase.execute(businessId, customerId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/{customerId}", version = "v1")
    @Operation(summary = "Atualizar classificação e notas internas do cliente", description = "Permite alterar o tier (ex: promover a VIP) e/ou as observações privadas da equipa sobre o cliente.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha do cliente atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de atualização inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ativo", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ficha do cliente não encontrada para este salão", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<BusinessCustomerResponse> updateCustomer(
            @PathVariable UUID businessId,
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateBusinessCustomerRequest request
    ) {
        BusinessCustomerResponse response = updateBusinessCustomerUseCase.execute(businessId, customerId, request);
        return ResponseEntity.ok(response);
    }
}
