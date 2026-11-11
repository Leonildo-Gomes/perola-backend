package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados completos do profissional no painel de gestão do salão")
public record BusinessEmployeeResponse(

        @Schema(description = "Identificador único do profissional", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identificador do salão ao qual pertence", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID businessId,

        @Schema(description = "Identificador do utilizador associado (opcional)", example = "7e6d5c4b-5717-4562-b3fc-2c963f66afa6", nullable = true)
        UUID userId,

        @Schema(description = "Nome de exibição do profissional", example = "Maria Silva")
        String displayName,

        @Schema(description = "Email da conta de utilizador associada", example = "maria@salao.cv", nullable = true)
        String email,

        @Schema(description = "Telefone da conta de utilizador associada", example = "+2389912345", nullable = true)
        String phone,

        @Schema(description = "URL do avatar do profissional", nullable = true)
        String avatarUrl,

        @Schema(description = "Papel de acesso do colaborador no salão (OWNER ou EMPLOYEE)", example = "EMPLOYEE", nullable = true)
        String accessRole,

        @Schema(description = "Título ou especialidade exibida no perfil do profissional", example = "Cabeleireira Sénior & Trancista", nullable = true)
        String specialty,

        @Schema(description = "Identificadores dos serviços atribuídos ao profissional")
        List<UUID> assignedServiceIds,

        @Schema(description = "Estado de ativação do profissional", example = "true")
        boolean isActive,

        @Schema(description = "Data e hora de criação em UTC", example = "2026-10-27T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Data e hora da última atualização em UTC", example = "2026-10-27T10:00:00Z")
        Instant updatedAt
) {
}

