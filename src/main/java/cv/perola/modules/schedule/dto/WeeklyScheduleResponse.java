package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Coleção completa dos horários semanais e pausas do profissional")
public record WeeklyScheduleResponse(

        @Schema(description = "Identificador do profissional", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Lista dos expedientes semanais configurados")
        List<WeeklyScheduleItemResponse> schedules
) {
}
