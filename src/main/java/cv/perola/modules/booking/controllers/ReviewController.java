package cv.perola.modules.booking.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.dto.CreateReviewRequest;
import cv.perola.modules.booking.dto.ReviewResponse;
import cv.perola.modules.booking.usecase.CreateReviewUseCase;
import cv.perola.modules.booking.usecase.ListBusinessReviewsUseCase;
import cv.perola.modules.booking.usecase.ListEmployeeReviewsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{version}")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Avaliações pós-serviço de clientes e consulta de avaliações por salão e profissional")
public class ReviewController {

        private final CreateReviewUseCase createReviewUseCase;
        private final ListBusinessReviewsUseCase listBusinessReviewsUseCase;
        private final ListEmployeeReviewsUseCase listEmployeeReviewsUseCase;

        @PostMapping(value = "/appointments/{id}/reviews", version = "v1")
        @SecurityRequirement(name = "BearerAuth")
        @Operation(summary = "Submeter avaliação de atendimento", description = "Regista a avaliação (1 a 5 estrelas) de um agendamento concluído (COMPLETED) pelo cliente autor.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Avaliação registada com sucesso", content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Agendamento não concluído ou nota fora do intervalo 1..5", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Apenas o cliente autor do agendamento pode avaliar", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "Já existe avaliação registada para este agendamento", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<ReviewResponse> createReview(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody CreateReviewRequest request) {
                if (principal == null) {
                        throw new AuthenticationFailedException("Autenticação necessária.");
                }
                ReviewResponse response = createReviewUseCase.execute(id, principal.getId(), request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping(value = "/businesses/{id}/reviews", version = "v1")
        @Operation(summary = "Listar avaliações de um salão", description = "Retorna a lista paginada de avaliações de atendimentos realizados no salão.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lista paginada de avaliações do salão"),
                        @ApiResponse(responseCode = "404", description = "Salão não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<PageResponse<ReviewResponse>> getBusinessReviews(
                        @PathVariable UUID id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResponse<ReviewResponse> response = listBusinessReviewsUseCase.execute(id, page, size);
                return ResponseEntity.ok(response);
        }

        @GetMapping(value = "/marketplace/employees/{id}/reviews", version = "v1")
        @Operation(summary = "Listar avaliações públicas de um profissional", description = "Retorna a lista paginada de avaliações de clientes para o profissional especificado.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lista paginada de avaliações do profissional"),
                        @ApiResponse(responseCode = "404", description = "Profissional não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<PageResponse<ReviewResponse>> getEmployeeReviews(
                        @PathVariable UUID id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResponse<ReviewResponse> response = listEmployeeReviewsUseCase.execute(id, page, size);
                return ResponseEntity.ok(response);
        }
}
