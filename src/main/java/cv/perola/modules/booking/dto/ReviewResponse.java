package cv.perola.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Representação de uma avaliação de atendimento pós-serviço")
public record ReviewResponse(

        @Schema(description = "Identificador único da avaliação", example = "9a8b7c6d-1122-3344-5566-77889900aabb")
        UUID id,

        @Schema(description = "Identificador da marcação avaliada", example = "8f3b2a1c-9988-4455-b3fc-2c963f66afa6")
        UUID appointmentId,

        @Schema(description = "Identificador do cliente autor da avaliação", example = "11223344-5566-7788-99aa-bbccddeeff00")
        UUID customerId,

        @Schema(description = "Identificador do profissional avaliado", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Nota atribuída (1 a 5)", example = "5")
        Integer rating,

        @Schema(description = "Comentário do cliente", example = "Excelente atendimento, super pontual e profissional!")
        String comment,

        @Schema(description = "Data e hora de criação da avaliação (ISO-8601 com offset)", example = "2026-11-03T11:30:00-01:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime createdAt
) {
}

