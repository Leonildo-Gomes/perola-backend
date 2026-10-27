package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados do feriado oficial")
public record HolidayResponse(

        @Schema(description = "Identificador único do feriado", example = "4e3d2c1b-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Data do feriado (YYYY-MM-DD)", example = "2026-01-01")
        LocalDate holidayDate,

        @Schema(description = "Nome oficial do feriado", example = "Ano Novo")
        String name,

        @Schema(description = "Indica se é um feriado de abrangência nacional", example = "true")
        boolean isNational
) {
}
