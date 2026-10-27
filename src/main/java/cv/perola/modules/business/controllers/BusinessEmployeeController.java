package cv.perola.modules.business.controllers;

import cv.perola.core.security.TenantFilter;
import cv.perola.modules.business.dto.AssignEmployeeServicesRequest;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.EmployeeServicesResponse;
import cv.perola.modules.business.dto.UpdateEmployeeRequest;
import cv.perola.modules.business.usecase.AssignEmployeeServicesUseCase;
import cv.perola.modules.business.usecase.CreateEmployeeUseCase;
import cv.perola.modules.business.usecase.GetEmployeeServicesUseCase;
import cv.perola.modules.business.usecase.ListEmployeesUseCase;
import cv.perola.modules.business.usecase.UpdateEmployeeUseCase;
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
@RequestMapping("/api/v1/businesses/{businessId}/employees")
@RequiredArgsConstructor
@Tag(name = "Salon Management", description = "Gestão da equipa de colaboradores do salão (Requer X-Business-Id)")
@SecurityRequirement(name = "BearerAuth")
public class BusinessEmployeeController {

    private final CreateEmployeeUseCase createEmployeeUseCase;
    private final ListEmployeesUseCase listEmployeesUseCase;
    private final UpdateEmployeeUseCase updateEmployeeUseCase;
    private final AssignEmployeeServicesUseCase assignEmployeeServicesUseCase;
    private final GetEmployeeServicesUseCase getEmployeeServicesUseCase;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar novo colaborador à equipa", description = "Regista um novo profissional no salão e atribui papel EMPLOYEE caso possua conta de utilizador associada.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Colaborador adicionado com sucesso",
                    content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao salão"),
            @ApiResponse(responseCode = "409", description = "Utilizador já associado a um colaborador")
    })
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = createEmployeeUseCase.execute(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'EMPLOYEE')")
    @Operation(summary = "Listar profissionais do salão", description = "Retorna todos os profissionais ativos da equipa do salão.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de colaboradores",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EmployeeResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao salão")
    })
    public ResponseEntity<List<EmployeeResponse>> listEmployees(@PathVariable UUID businessId) {
        List<EmployeeResponse> employees = listEmployeesUseCase.execute(businessId);
        return ResponseEntity.ok(employees);
    }

    @PatchMapping("/{employeeId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Atualizar dados do profissional", description = "Permite alterar nome de exibição e estado de ativação.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colaborador atualizado",
                    content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado")
    })
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable UUID businessId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeResponse response = updateEmployeeUseCase.execute(businessId, employeeId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/services")
    @PreAuthorize("hasAnyRole('OWNER', 'EMPLOYEE')")
    @Operation(summary = "Ver serviços atribuídos ao colaborador", description = "Lista os serviços que o colaborador está habilitado a prestar.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviços atribuídos",
                    content = @Content(schema = @Schema(implementation = EmployeeServicesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado")
    })
    public ResponseEntity<EmployeeServicesResponse> getEmployeeServices(
            @PathVariable UUID businessId,
            @PathVariable UUID employeeId) {
        EmployeeServicesResponse response = getEmployeeServicesUseCase.execute(businessId, employeeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/services")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Atribuir serviços que o colaborador executa", description = "Substitui a lista de serviços que o colaborador está apto a realizar.")
    @Parameter(name = TenantFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true, description = "UUID do salão ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviços atribuídos com sucesso",
                    content = @Content(schema = @Schema(implementation = EmployeeServicesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Colaborador ou serviço não encontrado")
    })
    public ResponseEntity<EmployeeServicesResponse> assignServices(
            @PathVariable UUID businessId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody AssignEmployeeServicesRequest request) {
        EmployeeServicesResponse response = assignEmployeeServicesUseCase.execute(businessId, employeeId, request);
        return ResponseEntity.ok(response);
    }
}
