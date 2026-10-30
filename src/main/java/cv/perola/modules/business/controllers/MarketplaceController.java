package cv.perola.modules.business.controllers;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessDetailResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.dto.MarketplaceServiceResponse;
import cv.perola.modules.business.usecase.business.GetMarketplaceBusinessDetailUseCase;
import cv.perola.modules.business.usecase.business.SearchMarketplaceBusinessesUseCase;
import cv.perola.modules.business.usecase.service.SearchMarketplaceServicesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Catálogo público de negócios, profissionais e serviços (Sem autenticação)")
public class MarketplaceController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SearchMarketplaceBusinessesUseCase searchMarketplaceBusinessesUseCase;
    private final GetMarketplaceBusinessDetailUseCase getMarketplaceBusinessDetailUseCase;
    private final SearchMarketplaceServicesUseCase searchMarketplaceServicesUseCase;

    @GetMapping(value = "/businesses", version = "v1")
    @Operation(summary = "Listar e pesquisar salões no marketplace", description = "Endpoint público para busca de salões ativos por nome, ilha ou cidade com paginação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de salões encontrados")
    })
    public ResponseEntity<PageResponse<MarketplaceBusinessSummaryResponse>> searchBusinesses(
            @Parameter(description = "Termo de pesquisa (nome do salão ou localização)") @RequestParam(required = false) String search,
            @Parameter(description = "Alias para termo de pesquisa") @RequestParam(required = false) String query,
            @Parameter(description = "Filtro por ilha (ex: Santiago, São Vicente, Sal)") @RequestParam(required = false) String island,
            @Parameter(description = "Filtro por cidade ou concelho (ex: Praia, Mindelo)") @RequestParam(required = false) String city,
            @Parameter(description = "Número da página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (máximo 50)") @RequestParam(defaultValue = "20") int size) {

        String searchTerm = resolveSearchTerm(search, query);
        Pageable pageable = createPageable(page, size, Sort.by("name").ascending());

        PageResponse<MarketplaceBusinessSummaryResponse> response = searchMarketplaceBusinessesUseCase.execute(
                searchTerm, island, city, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/businesses/{id}", version = "v1")
    @Operation(summary = "Perfil público de um salão", description = "Retorna os detalhes públicos de um salão ativo, incluindo catálogo de serviços e profissionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do salão encontrados", content = @Content(schema = @Schema(implementation = MarketplaceBusinessDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Salão não encontrado ou inativo")
    })
    public ResponseEntity<MarketplaceBusinessDetailResponse> getBusinessDetail(
            @Parameter(description = "Identificador único do salão (UUID)") @PathVariable UUID id) {
        MarketplaceBusinessDetailResponse response = getMarketplaceBusinessDetailUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/services", version = "v1")
    @Operation(summary = "Pesquisar serviços no catálogo público", description = "Busca paginada de serviços ativos de salões ativos, filtrando opcionalmente por nome/descrição, ilha e cidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de serviços encontrados")
    })
    public ResponseEntity<PageResponse<MarketplaceServiceResponse>> searchServices(
            @Parameter(description = "Termo de pesquisa por nome ou descrição do serviço")
            @RequestParam(required = false) String search,
            @Parameter(description = "Alias para termo de pesquisa")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filtro por ilha do salão")
            @RequestParam(required = false) String island,
            @Parameter(description = "Filtro por cidade do salão")
            @RequestParam(required = false) String city,
            @Parameter(description = "Número da página (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (máximo 50)")
            @RequestParam(defaultValue = "20") int size) {

        String searchTerm = resolveSearchTerm(search, query);
        Pageable pageable = createPageable(page, size, Sort.by("name").ascending());

        PageResponse<MarketplaceServiceResponse> response = searchMarketplaceServicesUseCase.execute(
                searchTerm, island, city, pageable
        );
        return ResponseEntity.ok(response);
    }

    private String resolveSearchTerm(String search, String query) {
        if (search != null && !search.trim().isBlank()) {
            return search.trim();
        }
        if (query != null && !query.trim().isBlank()) {
            return query.trim();
        }
        return null;
    }

    private Pageable createPageable(int page, int size, Sort sort) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(boundedPage, boundedSize, sort);
    }
}