package cv.perola.modules.business.usecase;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final ServiceMapper serviceMapper;

    @Transactional
    public ServiceResponse execute(UUID businessId, CreateServiceRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Salão não encontrado: " + businessId);
        }

        String serviceName = request.name().trim();
        if (serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue(serviceName, businessId)) {
            throw new ConflictException("Já existe um serviço ativo com o nome '" + serviceName + "' neste salão.");
        }

        ServiceEntity service = serviceMapper.toEntity(request, businessId);
        ServiceEntity saved = serviceRepository.save(service);

        return serviceMapper.toResponse(saved);
    }
}
