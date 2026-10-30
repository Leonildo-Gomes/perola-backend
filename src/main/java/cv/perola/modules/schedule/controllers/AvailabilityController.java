package cv.perola.modules.schedule.controllers;

import cv.perola.modules.schedule.dto.AvailabilitySlotsResponse;
import cv.perola.modules.schedule.usecase.CalculateAvailableSlotsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
@Tag(name = "Schedule & Availability", description = "Expediente de trabalho, pausas, abertura mensal e motor de slots livres")
public class AvailabilityController {

    private final CalculateAvailableSlotsUseCase calculateAvailableSlotsUseCase;

    @GetMapping("/slots")
    @Operation(
            summary = "Calcular slots livres para marcação",
            description = "Executa o algoritmo determinístico de corte de janelas de expediente e subtrai pausas, ausências, feriados e marcações ativas."
    )
    @ApiResponse(responseCode = "200", description = "Slots disponíveis calculados com sucesso")
    @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos ou ausentes")
    @ApiResponse(responseCode = "404", description = "Profissional, salão ou serviço não encontrado")
    public ResponseEntity<AvailabilitySlotsResponse> getAvailableSlots(
            @Parameter(description = "Identificador opcional do salão")
            @RequestParam(required = false) UUID businessId,
            @Parameter(description = "Identificador do profissional", required = true)
            @RequestParam UUID employeeId,
            @Parameter(description = "Identificador do serviço", required = true)
            @RequestParam UUID serviceId,
            @Parameter(description = "Data pretendida no formato YYYY-MM-DD", required = true, example = "2026-11-03")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AvailabilitySlotsResponse response = calculateAvailableSlotsUseCase.execute(businessId, employeeId, serviceId, date);
        return ResponseEntity.ok(response);
    }
}

