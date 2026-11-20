package cv.perola.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Resumo detalhado de um cliente da carteira do salão com métricas agregadas")
public record BusinessCustomerResponse(
        @Schema(description = "Identificador único do cliente")
        UUID customerId,

        @Schema(description = "Nome do cliente", example = "Márcia Gonçalves")
        String name,

        @Schema(description = "Email do cliente", example = "marcia.goncalves@exemplo.cv")
        String email,

        @Schema(description = "Telefone do cliente", example = "+2389912345")
        String phone,

        @Schema(description = "Indica se o cliente possui conta registada na plataforma", example = "true")
        boolean hasRegisteredAccount,

        @Schema(description = "Classificação CRM do cliente no salão", example = "VIP")
        CustomerTier tier,

        @Schema(description = "Notas internas privadas do salão", example = "Cliente VIP. Prefere atendimento com a Helena.", nullable = true)
        String internalNotes,

        @Schema(description = "Total de agendamentos realizados no salão", example = "14")
        long totalAppointments,

        @Schema(description = "Total de agendamentos concluídos no salão", example = "13")
        long completedAppointments,

        @Schema(description = "Total de agendamentos cancelados no salão", example = "1")
        long cancelledAppointments,

        @Schema(description = "Total gasto no salão em CVE", example = "32500.00")
        BigDecimal totalSpentCve,

        @Schema(description = "Data e hora do último agendamento (ISO-8601 com offset)", example = "2026-11-10T15:00:00Z", nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime lastAppointmentAt,

        @Schema(description = "Data e hora de criação do registo (ISO-8601 com offset)", example = "2026-03-12T09:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime createdAt
) {
}
