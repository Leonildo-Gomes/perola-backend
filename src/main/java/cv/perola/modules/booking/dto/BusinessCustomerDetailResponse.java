package cv.perola.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detalhe completo da ficha do cliente no salão acompanhado do histórico recente de marcações")
public record BusinessCustomerDetailResponse(
                @Schema(description = "Ficha do cliente no salão") BusinessCustomerResponse customer,

                @Schema(description = "Últimas marcações do cliente neste salão") List<AppointmentResponse> recentAppointments) {
}
