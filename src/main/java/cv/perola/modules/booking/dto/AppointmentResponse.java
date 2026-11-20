package cv.perola.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados detalhados de um agendamento")
public record AppointmentResponse(
                @Schema(description = "Identificador único do agendamento") UUID id,

                @Schema(description = "Identificador do salão") UUID businessId,

                @Schema(description = "Nome do salão", nullable = true) String businessName,

                @Schema(description = "Identificador do cliente") UUID customerId,

                @Schema(description = "Nome do cliente", nullable = true) String customerName,

                @Schema(description = "Email do cliente", nullable = true) String customerEmail,

                @Schema(description = "Telefone do cliente", nullable = true) String customerPhone,

                @Schema(description = "Identificador do profissional") UUID employeeId,

                @Schema(description = "Nome do profissional", nullable = true) String employeeName,

                @Schema(description = "Identificador do serviço") UUID serviceId,

                @Schema(description = "Snapshot do nome do serviço no momento da reserva") String serviceName,

                @Schema(description = "Snapshot do preço do serviço em CVE no momento da reserva") BigDecimal servicePrice,

                @Schema(description = "Snapshot da duração do serviço em minutos no momento da reserva") Integer serviceDuration,

                @Schema(description = "Data e hora de início do agendamento em ISO-8601 com offset", example = "2026-11-03T10:00:00-01:00") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime startTime,

                @Schema(description = "Data e hora de término do agendamento em ISO-8601 com offset", example = "2026-11-03T11:00:00-01:00") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime endTime,

                @Schema(description = "Data e hora de início proposta em caso de pedido de reagendamento", nullable = true) @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime proposedStartTime,

                @Schema(description = "Data e hora de término proposta em caso de pedido de reagendamento", nullable = true) @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime proposedEndTime,

                @Schema(description = "Estado atual do agendamento") AppointmentStatus status,

                @Schema(description = "Classificação CRM do cliente no salão (visível apenas para gestão)", nullable = true) CustomerTier customerTier,

                @Schema(description = "Notas internas privadas do salão sobre o cliente (visível apenas para gestão)", nullable = true) String customerInternalNotes,

                @Schema(description = "Notas ou observações do agendamento", nullable = true) String notes,

                @Schema(description = "Data e hora de criação do registo em ISO-8601 com offset") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime createdAt,

                @Schema(description = "Linha do tempo de auditoria das transições do agendamento", nullable = true) List<AppointmentHistoryResponse> history) {
        public AppointmentResponse(
                        UUID id,
                        UUID businessId,
                        UUID customerId,
                        UUID employeeId,
                        UUID serviceId,
                        String serviceName,
                        BigDecimal servicePrice,
                        Integer serviceDuration,
                        OffsetDateTime startTime,
                        OffsetDateTime endTime,
                        OffsetDateTime proposedStartTime,
                        OffsetDateTime proposedEndTime,
                        AppointmentStatus status,
                        String notes,
                        OffsetDateTime createdAt,
                        List<AppointmentHistoryResponse> history) {
                this(
                                id,
                                businessId,
                                null,
                                customerId,
                                null,
                                null,
                                null,
                                employeeId,
                                null,
                                serviceId,
                                serviceName,
                                servicePrice,
                                serviceDuration,
                                startTime,
                                endTime,
                                proposedStartTime,
                                proposedEndTime,
                                status,
                                null,
                                null,
                                notes,
                                createdAt,
                                history != null ? history : List.of());
        }

        public AppointmentResponse(
                        UUID id,
                        UUID businessId,
                        UUID customerId,
                        UUID employeeId,
                        UUID serviceId,
                        String serviceName,
                        BigDecimal servicePrice,
                        Integer serviceDuration,
                        OffsetDateTime startTime,
                        OffsetDateTime endTime,
                        OffsetDateTime proposedStartTime,
                        OffsetDateTime proposedEndTime,
                        AppointmentStatus status,
                        String notes,
                        OffsetDateTime createdAt) {
                this(
                                id,
                                businessId,
                                customerId,
                                employeeId,
                                serviceId,
                                serviceName,
                                servicePrice,
                                serviceDuration,
                                startTime,
                                endTime,
                                proposedStartTime,
                                proposedEndTime,
                                status,
                                notes,
                                createdAt,
                                List.of());
        }

}
