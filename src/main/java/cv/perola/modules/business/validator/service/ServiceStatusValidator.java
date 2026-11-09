package cv.perola.modules.business.validator.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceStatusValidator {

    private final ServiceRepository serviceRepository;

    public void validateActivation(UUID businessId, ServiceEntity service) {
        String serviceName = service.getName().trim();
        if (serviceRepository.existsByNameIgnoreCaseAndBusinessIdAndIsActiveTrue(serviceName, businessId)) {
            throw new ConflictException("Já existe um serviço ativo com o nome '" + serviceName + "' neste salão.");
        }
    }
}
