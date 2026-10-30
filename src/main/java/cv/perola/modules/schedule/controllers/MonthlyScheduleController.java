package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.dto.MonthlyScheduleResponse;
import cv.perola.modules.schedule.dto.OpenMonthlyScheduleRequest;
import cv.perola.modules.schedule.usecase.monthly.GetMonthlyScheduleUseCase;
import cv.perola.modules.schedule.usecase.monthly.OpenMonthlyScheduleUseCase;
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
@RequestMapping("/api/{version}/schedule/employees/{employeeId}/monthly")
@RequiredArgsConstructor
@Tag(name = "Schedule & Availability", description = "Gestão de perfis de horário semanal, dias de trabalho, pausas e abertura mensal")
@SecurityRequirement(name = "BearerAuth")
public class MonthlyScheduleController {

    private final OpenMonthlyScheduleUseCase openMonthlyScheduleUseCase;
    private final GetMonthlyScheduleUseCase getMonthlyScheduleUseCase;

    @PostMapping(value = "/open", version = "v1")
    @Operation(
            summary = "Abrir agenda mensal de atendimentos",
            description = "Ativa deliberadamente a agenda de um colaborador para um mês/ano específico, vinculando o perfil de horário semanal escolhido e registando decisões de folga em feriados."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda mensal aberta com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, horário inativo ou feriado fora do mês",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador ou perfil de horário não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MonthlyScheduleResponse> openMonthlySchedule(
            @PathVariable UUID employeeId,
            @Valid @RequestBody OpenMonthlyScheduleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MonthlyScheduleResponse response = openMonthlyScheduleUseCase.execute(employeeId, request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Consultar status de abertura mensal e horário associado",
            description = "Consulta os dados de abertura mensal e o perfil de horário semanal associado a um colaborador para o par (year, month)."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de abertura mensal obtido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de ano ou mês inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado ou agenda mensal ainda não aberta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MonthlyScheduleResponse> getMonthlySchedule(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MonthlyScheduleResponse response = getMonthlyScheduleUseCase.execute(employeeId, year, month, principal);
        return ResponseEntity.ok(response);
    }
}

