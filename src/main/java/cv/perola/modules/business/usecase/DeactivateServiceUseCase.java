package cv.perola.modules.business.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeactivateServiceUseCase {

    private final ServiceRepository serviceRepository;

    @Transactional
    public void execute(UUID businessId, UUID serviceId) {
        ServiceEntity service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + serviceId));

        service.setActive(false);
        serviceRepository.save(service);
    }
}
