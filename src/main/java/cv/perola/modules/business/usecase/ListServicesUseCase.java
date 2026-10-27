package cv.perola.modules.business.usecase;

import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListServicesUseCase {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    @Transactional(readOnly = true)
    public List<ServiceResponse> execute(UUID businessId) {
        return serviceRepository.findAllByBusinessIdAndIsActiveTrue(businessId)
                .stream()
                .map(serviceMapper::toResponse)
                .toList();
    }
}
