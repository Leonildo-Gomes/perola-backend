package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Payload para configuração completa da rotina semanal de horários e pausas do profissional")
public record SetWeeklyScheduleRequest(

        @NotNull(message = "A lista de horários semanais é obrigatória.")
        @NotEmpty(message = "Pelo menos um dia da semana deve ser configurado.")
        @Valid
        @Schema(description = "Lista de horários por dia da semana")
        List<WeeklyScheduleItemRequest> schedules
) {
}
