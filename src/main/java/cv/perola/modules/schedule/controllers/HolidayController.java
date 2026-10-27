package cv.perola.modules.schedule.controllers;

import cv.perola.modules.schedule.dto.HolidayResponse;
import cv.perola.modules.schedule.usecase.ListHolidaysUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule/holidays")
@RequiredArgsConstructor
@Tag(name = "Schedule & Availability", description = "Consulta de feriados oficiais e gestão de agenda")
public class HolidayController {

    private final ListHolidaysUseCase listHolidaysUseCase;

    @GetMapping
    @Operation(
            summary = "Consultar feriados oficiais de Cabo Verde",
            description = "Retorna a listagem de feriados oficiais nacionais de Cabo Verde, opcionalmente filtrados por ano. Endpoint público."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de feriados retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = HolidayResponse.class)))
    )
    public ResponseEntity<List<HolidayResponse>> listHolidays(
            @Parameter(description = "Ano de referência (ex: 2026)", example = "2026")
            @RequestParam(required = false) Integer year
    ) {
        List<HolidayResponse> holidays = listHolidaysUseCase.execute(year);
        return ResponseEntity.ok(holidays);
    }
}
