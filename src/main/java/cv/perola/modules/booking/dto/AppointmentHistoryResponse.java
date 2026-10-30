package cv.perola.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Registo imutável da linha do tempo de auditoria de um agendamento")
public record AppointmentHistoryResponse(
        @Schema(description = "Identificador único do registo de histórico")
        UUID id,

        @Schema(description = "Identificador do agendamento")
        UUID appointmentId,

        @Schema(description = "Identificador do utilizador autor da ação")
        UUID performedById,

        @Schema(description = "Ação realizada")
        AppointmentAction action,

        @Schema(description = "Estado anterior à transição", nullable = true)
        AppointmentStatus fromStatus,

        @Schema(description = "Novo estado resultante da transição")
        AppointmentStatus toStatus,

        @Schema(description = "Horário de início anterior", nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime oldStartTime,

        @Schema(description = "Horário de término anterior", nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime oldEndTime,

        @Schema(description = "Novo horário de início proposto ou consolidado", nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime newStartTime,

        @Schema(description = "Novo horário de término proposto ou consolidado", nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime newEndTime,

        @Schema(description = "Notas ou motivo associado à transição", nullable = true)
        String notes,

        @Schema(description = "Data e hora de registo da ação em ISO-8601 com offset")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime createdAt
) {
}

