package cv.perola.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Resposta contendo os serviços atribuídos a um profissional")
public record EmployeeServicesResponse(

        @Schema(description = "Identificador do profissional", example = "1a2b3c4d-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Identificador do salão", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID businessId,

        @Schema(description = "Lista de serviços que o profissional executa")
        List<ServiceResponse> services
) {
}
