package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Representação detalhada do estabelecimento")
public record BusinessResponse(
        @Schema(description = "Identificador único do salão (Tenant ID)") UUID id,

        @Schema(description = "Identificador do utilizador proprietário") UUID ownerId,

        @Schema(description = "Nome comercial do salão") String name,

        @Schema(description = "Ilha onde o salão se localiza") String island,

        @Schema(description = "Cidade ou concelho") String city,

        @Schema(description = "Morada completa ou ponto de referência") String location,

        @Schema(description = "Telefone de contacto do salão") String contactPhone,

        @Schema(description = "URL do logótipo do salão", nullable = true) String logoUrl,

        @Schema(description = "Fuso horário IANA oficial") String timezone,

        @Schema(description = "Categorias de atuação do estabelecimento") List<BusinessCategoryResponse> categories,

        @Schema(description = "Indica se o estabelecimento está ativo") boolean isActive,

        @Schema(description = "Data/hora de registo em UTC") Instant createdAt,
        @Schema(description = "Data/hora de registo em UTC") Instant updatedAt) {
    public BusinessResponse(
            UUID id,
            UUID ownerId,
            String name,
            String island,
            String city,
            String location,
            String contactPhone,
            String timezone,
            List<BusinessCategoryResponse> categories,
            boolean isActive,
            Instant createdAt,
            Instant updatedAt) {
        this(id, ownerId, name, island, city, location, contactPhone, null, timezone, categories, isActive, createdAt,
                updatedAt);
    }

}
