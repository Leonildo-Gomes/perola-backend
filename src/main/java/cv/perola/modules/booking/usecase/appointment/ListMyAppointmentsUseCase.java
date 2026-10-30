package cv.perola.modules.booking.usecase.appointment;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.booking.domain.AppointmentEntity;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import cv.perola.modules.booking.dto.AppointmentResponse;
import cv.perola.modules.booking.mapper.AppointmentMapper;
import cv.perola.modules.booking.repository.AppointmentRepository;
import cv.perola.modules.booking.service.AppointmentLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para listar o histórico paginado de agendamentos do cliente autenticado.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMyAppointmentsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentLifecycleService lifecycleService;

    public PageResponse<AppointmentResponse> execute(
            UUID customerId,
            AppointmentStatus status,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<AppointmentEntity> resultPage = appointmentRepository.findCustomerAppointments(
                customerId,
                status,
                PageRequest.of(safePage, safeSize)
        );

        List<AppointmentResponse> content = resultPage.getContent().stream()
                .map(entity -> {
                    ZoneId zoneId = lifecycleService.resolveBusinessZone(entity.getBusinessId());
                    return appointmentMapper.toResponse(entity, zoneId);
                })
                .toList();

        return new PageResponse<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }
}

