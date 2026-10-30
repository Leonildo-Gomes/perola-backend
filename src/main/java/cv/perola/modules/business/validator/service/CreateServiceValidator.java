package cv.perola.modules.business.validator.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.dto.CreateServiceRequest;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateServiceValidator {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;

    public void validate(UUID businessId, CreateServiceRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Salão não encontrado: " + businessId);
        }

        String serviceName = request.name().trim();
        if (serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue(serviceName, businessId)) {
            throw new ConflictException("Já existe um serviço ativo com o nome '" + serviceName + "' neste salão.");
        }
    }
}

