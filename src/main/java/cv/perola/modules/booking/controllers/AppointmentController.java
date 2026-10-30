package cv.perola.modules.booking.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.UserPrincipal;
import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentActionNotesRequest;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.dto.CancelAppointmentRequest;
import cv.perola.modules.booking.dto.CreateAppointmentRequest;
import cv.perola.modules.booking.dto.RejectRescheduleRequest;
import cv.perola.modules.booking.dto.RescheduleAppointmentRequest;
import cv.perola.modules.booking.usecase.AcceptRescheduleUseCase;
import cv.perola.modules.booking.usecase.CancelAppointmentUseCase;
import cv.perola.modules.booking.usecase.CompleteAppointmentUseCase;
import cv.perola.modules.booking.usecase.ConfirmAppointmentUseCase;
import cv.perola.modules.booking.usecase.CreateAppointmentUseCase;
import cv.perola.modules.booking.usecase.GetAppointmentUseCase;
import cv.perola.modules.booking.usecase.ListMyAppointmentsUseCase;
import cv.perola.modules.booking.usecase.ListSalonAppointmentsUseCase;
import cv.perola.modules.booking.usecase.ProposeRescheduleUseCase;
import cv.perola.modules.booking.usecase.RejectRescheduleUseCase;
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
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/appointments")
@RequiredArgsConstructor
@Tag(name = "Booking & Appointments", description = "Reservas, fluxo de aprovação, reagendamento e cancelamentos")
@SecurityRequirement(name = "BearerAuth")
public class AppointmentController {

        private final CreateAppointmentUseCase createAppointmentUseCase;
        private final GetAppointmentUseCase getAppointmentUseCase;
        private final ConfirmAppointmentUseCase confirmAppointmentUseCase;
        private final CompleteAppointmentUseCase completeAppointmentUseCase;
        private final CancelAppointmentUseCase cancelAppointmentUseCase;
        private final ProposeRescheduleUseCase proposeRescheduleUseCase;
        private final AcceptRescheduleUseCase acceptRescheduleUseCase;
        private final RejectRescheduleUseCase rejectRescheduleUseCase;
        private final ListMyAppointmentsUseCase listMyAppointmentsUseCase;
        private final ListSalonAppointmentsUseCase listSalonAppointmentsUseCase;

        @PostMapping(version = "v1")
        @Operation(summary = "Realizar novo agendamento", description = "Reserva um horário específico com snapshot de serviço e garantia atómica anti-overbooking GiST.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso", content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Salão, profissional ou serviço não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "Conflito de horário — o profissional já possui marcação ativa neste intervalo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> createAppointment(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody CreateAppointmentRequest request) {
                requirePrincipal(principal);
                AppointmentResponse response = createAppointmentUseCase.execute(principal.getId(), request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping(version = "v1")
        @Operation(summary = "Listar agenda de marcações do salão", description = "Retorna a lista paginada de agendamentos do salão ativo (requer cabeçalho X-Business-Id).")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Coleção paginada de marcações da agenda"),
                        @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<PageResponse<AppointmentResponse>> getSalonAppointments(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @RequestParam(required = false) UUID employeeId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(required = false) AppointmentStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                requirePrincipal(principal);
                PageResponse<AppointmentResponse> response = listSalonAppointmentsUseCase.execute(
                                employeeId, startDate, endDate, status, page, size, principal);
                return ResponseEntity.ok(response);
        }

        @GetMapping(value = "/my", version = "v1")
        @Operation(summary = "Consultar histórico de marcações do cliente autenticado", description = "Retorna os agendamentos pessoais do cliente autenticado ordenados cronologicamente.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Histórico de agendamentos pessoais"),
                        @ApiResponse(responseCode = "401", description = "Autenticação obrigatória", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<PageResponse<AppointmentResponse>> getMyAppointments(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @RequestParam(required = false) AppointmentStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                requirePrincipal(principal);
                PageResponse<AppointmentResponse> response = listMyAppointmentsUseCase.execute(
                                principal.getId(), status, page, size);
                return ResponseEntity.ok(response);
        }

        @GetMapping(value = "/{id}", version = "v1")
        @Operation(summary = "Obter detalhes completos da marcação e linha do tempo", description = "Retorna os detalhes do agendamento e todo o histórico imutável de auditoria (append-only).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Detalhes do agendamento obtidos com sucesso"),
                        @ApiResponse(responseCode = "403", description = "Sem permissão para aceder a este agendamento", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> getAppointmentById(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                AppointmentResponse response = getAppointmentUseCase.execute(id, principal);
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/{id}/confirm", version = "v1")
        @Operation(summary = "Confirmar agendamento pendente", description = "Transita o agendamento de PENDING para CONFIRMED. Requer cabeçalho X-Business-Id e permissão de OWNER ou EMPLOYEE responsável.")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Agendamento confirmado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Transição de estado inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> confirmAppointment(
                        @PathVariable UUID id,
                        @Valid @RequestBody(required = false) AppointmentActionNotesRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                String notes = request != null ? request.notes() : null;
                AppointmentResponse response = confirmAppointmentUseCase.execute(id, notes, principal);
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/{id}/complete", version = "v1")
        @Operation(summary = "Marcar atendimento como concluído", description = "Transita o agendamento de CONFIRMED para COMPLETED. Requer cabeçalho X-Business-Id e permissão de OWNER ou EMPLOYEE responsável.")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Atendimento concluído com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Transição de estado inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> completeAppointment(
                        @PathVariable UUID id,
                        @Valid @RequestBody(required = false) AppointmentActionNotesRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                String notes = request != null ? request.notes() : null;
                AppointmentResponse response = completeAppointmentUseCase.execute(id, notes, principal);
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/{id}/cancel", version = "v1")
        @Operation(summary = "Cancelar marcação", description = "Cancela o agendamento (PENDING, CONFIRMED ou RESCHEDULE_PENDING -> CANCELLED), libertando o horário na constraint GiST.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Agendamento cancelado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Transição de estado inválida (ex.: já concluído ou cancelado)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Sem permissão para cancelar este agendamento", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> cancelAppointment(
                        @PathVariable UUID id,
                        @Valid @RequestBody(required = false) CancelAppointmentRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                String reason = request != null ? request.reason() : null;
                AppointmentResponse response = cancelAppointmentUseCase.execute(id, reason, principal);
                return ResponseEntity.ok(response);
        }

        @PatchMapping(value = "/{id}/reschedule", version = "v1")
        @Operation(summary = "Propor novo horário de reagendamento", description = "Regista uma proposta de reagendamento (CONFIRMED -> RESCHEDULE_PENDING), mantendo o horário original reservado.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Proposta de reagendamento registada"),
                        @ApiResponse(responseCode = "400", description = "Transição de estado inválida ou dados incorretos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Sem permissão para reagendar esta marcação", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> proposeReschedule(
                        @PathVariable UUID id,
                        @Valid @RequestBody RescheduleAppointmentRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                AppointmentResponse response = proposeRescheduleUseCase.execute(id, request, principal);
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/{id}/reschedule/accept", version = "v1")
        @Operation(summary = "Aceitar proposta de reagendamento", description = "Efetiva a proposta de reagendamento (RESCHEDULE_PENDING -> CONFIRMED), atualizando o horário vigente sob proteção GiST anti-overbooking.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Reagendamento aceite e confirmado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Agendamento não possui proposta de reagendamento pendente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Sem permissão para aceitar este reagendamento", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "O horário proposto foi entretanto ocupado (violação GiST)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> acceptReschedule(
                        @PathVariable UUID id,
                        @Valid @RequestBody(required = false) AppointmentActionNotesRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                String notes = request != null ? request.notes() : null;
                AppointmentResponse response = acceptRescheduleUseCase.execute(id, notes, principal);
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/{id}/reschedule/reject", version = "v1")
        @Operation(summary = "Recusar proposta de reagendamento", description = "Recusa a proposta de reagendamento (RESCHEDULE_PENDING -> CONFIRMED), mantendo o horário original intacto.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Proposta de reagendamento recusada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Agendamento não se encontra em RESCHEDULE_PENDING", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Sem permissão para recusar este reagendamento", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<AppointmentResponse> rejectReschedule(
                        @PathVariable UUID id,
                        @Valid @RequestBody(required = false) RejectRescheduleRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                requirePrincipal(principal);
                String reason = request != null ? request.reason() : null;
                AppointmentResponse response = rejectRescheduleUseCase.execute(id, reason, principal);
                return ResponseEntity.ok(response);
        }

        private void requirePrincipal(UserPrincipal principal) {
                if (principal == null) {
                        throw new AuthenticationFailedException("Autenticação necessária.");
                }
        }
}