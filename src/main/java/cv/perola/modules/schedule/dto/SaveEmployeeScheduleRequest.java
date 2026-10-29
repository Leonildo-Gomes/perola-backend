package cv.perola.modules.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Payload para criação ou atualização de um perfil de horário semanal (com os 7 dias da semana)")
public record SaveEmployeeScheduleRequest(

        @NotBlank(message = "O nome do perfil de horário é obrigatório.")
        @Size(max = 100, message = "O nome do perfil de horário deve ter no máximo 100 caracteres.")
        @Schema(description = "Nome do perfil de horário semanal", example = "Horário Padrão")
        String name,

        @Schema(description = "Indica se este é o perfil de horário padrão do profissional", example = "true", defaultValue = "false")
        Boolean isDefault,

        @Schema(description = "Indica se o perfil de horário está ativo", example = "true", defaultValue = "true")
        Boolean isActive,

        @NotNull(message = "A lista dos 7 dias da semana é obrigatória.")
        @Size(min = 7, max = 7, message = "O perfil de horário deve conter exatamente os 7 dias da semana (MONDAY a SUNDAY).")
        @Valid
        @Schema(description = "Configuração dos 7 dias da semana (MONDAY a SUNDAY)")
        List<ScheduleDayItemRequest> days
) {
    public boolean resolvedIsDefault() {
        return Boolean.TRUE.equals(isDefault);
    }

    public boolean resolvedIsActive() {
        return isActive == null || isActive;
    }
}

