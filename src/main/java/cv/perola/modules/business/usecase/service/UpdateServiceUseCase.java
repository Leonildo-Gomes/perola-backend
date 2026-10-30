package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    @Transactional
    public ServiceResponse execute(UUID businessId, UUID serviceId, UpdateServiceRequest request) {
        ServiceEntity service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + serviceId));

        if (request.name() != null) {
            String newName = request.name().trim();
            if (!newName.equalsIgnoreCase(service.getName())
                    && serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue(newName, businessId)) {
                throw new ConflictException("Já existe um serviço ativo com o nome '" + newName + "' neste salão.");
            }
            service.setName(newName);
        }

        if (request.description() != null) {
            service.setDescription(request.description().trim());
        }

        if (request.durationMinutes() != null) {
            service.setDurationMinutes(request.durationMinutes());
        }

        if (request.price() != null) {
            service.setPrice(request.price());
        }

        if (request.isActive() != null) {
            service.setActive(request.isActive());
        }

        ServiceEntity updated = serviceRepository.save(service);
        return serviceMapper.toResponse(updated);
    }
}
