package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.dto.SetWeeklyScheduleRequest;
import cv.perola.modules.schedule.dto.WeeklyScheduleResponse;
import cv.perola.modules.schedule.usecase.GetWeeklyScheduleUseCase;
import cv.perola.modules.schedule.usecase.SetWeeklyScheduleUseCase;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule/employees/{employeeId}/weekly")
@RequiredArgsConstructor
@Tag(name = "Schedule & Availability", description = "Gestão de horários, expediente semanal e pausas de colaboradores")
@SecurityRequirement(name = "BearerAuth")
public class ScheduleWeeklyController {

    private final SetWeeklyScheduleUseCase setWeeklyScheduleUseCase;
    private final GetWeeklyScheduleUseCase getWeeklyScheduleUseCase;

    @PutMapping
    @Operation(
            summary = "Configurar rotina semanal de horários e pausas",
            description = "Substitui atDrawalmente a rotina semanal de expediente e intervalos do colaborador. Requer papel OWNER ou o próprio EMPLOYEE."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rotina semanal configurada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou pausas inconsistentes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<WeeklyScheduleResponse> setWeeklySchedule(
            @PathVariable UUID employeeId,
            @Valid @RequestBody SetWeeklyScheduleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WeeklyScheduleResponse response = setWeeklyScheduleUseCase.execute(employeeId, request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Obter expediente semanal de rotina e pausas",
            description = "Consulta a configuração semanal de dias trabalhados e intervalos do colaborador. Requer papel OWNER ou o próprio EMPLOYEE."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expediente semanal obtido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<WeeklyScheduleResponse> getWeeklySchedule(
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WeeklyScheduleResponse response = getWeeklyScheduleUseCase.execute(employeeId, principal);
        return ResponseEntity.ok(response);
    }
}
