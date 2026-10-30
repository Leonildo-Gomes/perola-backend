package cv.perola.modules.business.usecase;

import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.validator.service.CreateServiceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final CreateServiceValidator createServiceValidator;

    @Transactional
    public ServiceResponse execute(UUID businessId, CreateServiceRequest request) {
        createServiceValidator.validate(businessId, request);

        ServiceEntity service = serviceMapper.toEntity(request, businessId);
        ServiceEntity saved = serviceRepository.save(service);

        return serviceMapper.toResponse(saved);
    }
}
