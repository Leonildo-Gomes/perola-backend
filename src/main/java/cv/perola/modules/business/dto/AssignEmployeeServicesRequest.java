package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Payload para atribuir catálogo de serviços que o profissional está habilitado a executar")
public record AssignEmployeeServicesRequest(

        @Schema(description = "Lista de identificadores dos serviços atribuídos")
        @NotNull(message = "A lista de identificadores de serviços é obrigatória")
        List<UUID> serviceIds
) {
}
