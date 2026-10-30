package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.dto.CreateTimeOffRequest;
import cv.perola.modules.schedule.dto.TimeOffResponse;
import cv.perola.modules.schedule.usecase.timeoff.CreateTimeOffUseCase;
import cv.perola.modules.schedule.usecase.timeoff.DeleteTimeOffUseCase;
import cv.perola.modules.schedule.usecase.timeoff.ListTimeOffUseCase;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/schedule/employees/{employeeId}/time-off")
@RequiredArgsConstructor
@Tag(name = "Schedule & Availability", description = "Gestão de ausências, férias e bloqueios de agenda dos colaboradores")
@SecurityRequirement(name = "BearerAuth")
public class ScheduleTimeOffController {

    private final CreateTimeOffUseCase createTimeOffUseCase;
    private final ListTimeOffUseCase listTimeOffUseCase;
    private final DeleteTimeOffUseCase deleteTimeOffUseCase;

    @PostMapping(version = "v1")
    @Operation(
            summary = "Registar férias, consulta médica ou folga pontual",
            description = "Cria um registo de ausência ou bloqueio para o colaborador no salão. Requer papel OWNER ou o próprio EMPLOYEE."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ausência registada com sucesso",
                    content = @Content(schema = @Schema(implementation = TimeOffResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datas ou horários inconsistentes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TimeOffResponse> createTimeOff(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateTimeOffRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TimeOffResponse response = createTimeOffUseCase.execute(employeeId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Listar ausências, férias e bloqueios do profissional",
            description = "Consulta a lista cronológica de períodos de ausência do colaborador. Requer papel OWNER ou o próprio EMPLOYEE."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de ausências retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TimeOffResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TimeOffResponse>> listTimeOff(
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TimeOffResponse> list = listTimeOffUseCase.execute(employeeId, principal);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping(value = "/{timeOffId}", version = "v1")
    @Operation(
            summary = "Remover bloqueio de ausência ou férias",
            description = "Remove um período de ausência previamente registado, desbloqueando a agenda. Requer papel OWNER ou o próprio EMPLOYEE."
    )
    @Parameter(
            name = TenantFilter.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "Identificador único do salão (UUID)",
            schema = @Schema(type = "string", format = "uuid")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ausência removida com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador ou registo de ausência não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteTimeOff(
            @PathVariable UUID employeeId,
            @PathVariable UUID timeOffId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        deleteTimeOffUseCase.execute(employeeId, timeOffId, principal);
        return ResponseEntity.noContent().build();
    }
}
