package cv.perola.modules.schedule.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.usecase.schedule.CreateEmployeeScheduleUseCase;
import cv.perola.modules.schedule.usecase.schedule.GetEmployeeScheduleUseCase;
import cv.perola.modules.schedule.usecase.schedule.ListEmployeeSchedulesUseCase;
import cv.perola.modules.schedule.usecase.schedule.UpdateEmployeeScheduleUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/{version}/schedule/employees/{employeeId}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule & Availability", description = "Gestão de perfis de horário semanal, dias de trabalho, pausas e abertura mensal")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeScheduleController {

        private final CreateEmployeeScheduleUseCase createEmployeeScheduleUseCase;
        private final ListEmployeeSchedulesUseCase listEmployeeSchedulesUseCase;
        private final GetEmployeeScheduleUseCase getEmployeeScheduleUseCase;
        private final UpdateEmployeeScheduleUseCase updateEmployeeScheduleUseCase;

        @PostMapping(version = "v1")
        @Operation(summary = "Criar novo perfil de horário semanal (com os 7 dias e pausas)", description = "Cria um perfil/template de horário semanal contendo obrigatoriamente os 7 dias da semana (MONDAY a SUNDAY). Requer papel OWNER ou o próprio EMPLOYEE.")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Perfil de horário semanal criado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos, menos de 7 dias ou pausas inconsistentes", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Colaborador não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<EmployeeScheduleResponse> createSchedule(
                        @PathVariable UUID employeeId,
                        @Valid @RequestBody SaveEmployeeScheduleRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                EmployeeScheduleResponse response = createEmployeeScheduleUseCase.execute(employeeId, request,
                                principal);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping(version = "v1")
        @Operation(summary = "Listar perfis de horário semanal do profissional", description = "Lista todos os perfis de horário semanal criados pelo colaborador com os respetivos 7 dias e pausas. Requer papel OWNER ou o próprio EMPLOYEE.")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Perfis de horário obtidos com sucesso"),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Colaborador não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<List<EmployeeScheduleResponse>> listSchedules(
                        @PathVariable UUID employeeId,
                        @AuthenticationPrincipal UserPrincipal principal) {
                List<EmployeeScheduleResponse> response = listEmployeeSchedulesUseCase.execute(employeeId, principal);
                return ResponseEntity.ok(response);
        }

        @GetMapping(value = "/{scheduleId}", version = "v1")
        @Operation(summary = "Obter detalhes de um perfil de horário semanal específico", description = "Consulta um perfil de horário semanal pelo seu identificador, incluindo os 7 dias da semana e pausas.")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Perfil de horário obtido com sucesso"),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Colaborador ou perfil de horário não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<EmployeeScheduleResponse> getSchedule(
                        @PathVariable UUID employeeId,
                        @PathVariable UUID scheduleId,
                        @AuthenticationPrincipal UserPrincipal principal) {
                EmployeeScheduleResponse response = getEmployeeScheduleUseCase.execute(employeeId, scheduleId,
                                principal);
                return ResponseEntity.ok(response);
        }

        @PutMapping(value = "/{scheduleId}", version = "v1")
        @Operation(summary = "Atualizar perfil de horário semanal existente (7 dias e pausas)", description = "Substitui de forma transacional a configuração dos 7 dias e pausas de um perfil de horário existente. Requer papel OWNER ou o próprio EMPLOYEE.")
        @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "Identificador único do salão (UUID)", schema = @Schema(type = "string", format = "uuid"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Perfil de horário atualizado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos ou pausas inconsistentes", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o salão ou colaborador", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Colaborador ou perfil de horário não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<EmployeeScheduleResponse> updateSchedule(
                        @PathVariable UUID employeeId,
                        @PathVariable UUID scheduleId,
                        @Valid @RequestBody SaveEmployeeScheduleRequest request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                EmployeeScheduleResponse response = updateEmployeeScheduleUseCase.execute(employeeId, scheduleId,
                                request, principal);
                return ResponseEntity.ok(response);
        }

}

        
                        
                         
                         
        
        
                        
                        
                        
                
                                
                
        

        
         
            
        
                        
                         
                         
                         
        
        
                        
                        
                        
                        
                
                                
                
        
