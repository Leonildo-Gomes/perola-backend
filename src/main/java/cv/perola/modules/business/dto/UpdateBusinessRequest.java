package cv.perola.modules.business.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dados para atualização das informações do salão")
public record UpdateBusinessRequest(
                @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.") @Schema(description = "Novo nome comercial do salão", example = "Salão Pérola VIP") String name,

                @Size(max = 50, message = "A ilha deve ter no máximo 50 caracteres.") @Schema(description = "Ilha de Cabo Verde", example = "Santiago") String island,

                @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.") @Schema(description = "Cidade ou concelho", example = "Praia") String city,

                @Schema(description = "Nova morada ou ponto de referência", example = "Avenida Cidade de Lisboa, Edifício Pérola") String location,

                @Size(max = 50, message = "O telefone deve ter no máximo 50 caracteres.") @Schema(description = "Telefone público de contacto", example = "+2382619999") String contactPhone,

                @Size(max = 1000, message = "O URL do logótipo não pode exceder 1000 caracteres.") @Schema(description = "URL ou storageKey do logótipo obtido via POST /api/v1/media/upload (scope BUSINESS_LOGO)", nullable = true) String logoUrl,

                @Schema(description = "Fuso horário IANA", example = "Atlantic/Cape_Verde") String timezone,

                @Valid @Schema(description = "Nova lista de categorias do estabelecimento (quando fornecida, exige pelo menos 1 item e exatamente 1 com isPrimary = true)") List<BusinessCategoryItemRequest> categories) {
        @JsonCreator
        public UpdateBusinessRequest {
        }

        public UpdateBusinessRequest(
                        String name,
                        String island,
                        String city,
                        String location,
                        String contactPhone,
                        String timezone,
                        List<BusinessCategoryItemRequest> categories) {
                this(name, island, city, location, contactPhone, null, timezone, categories);
        }
}
