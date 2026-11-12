package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.ValidationException;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateServiceRequest;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.CategoryRepository;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.ServiceAssignmentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;
    private final ServiceAssignmentLookupService serviceAssignmentLookupService;
    private final ServiceMapper serviceMapper;

    @Transactional
    public ServiceResponse execute(UUID businessId, UUID serviceId, UpdateServiceRequest request) {
        return execute(businessId, serviceId, request, false);
    }

    @Transactional
    public ServiceResponse execute(UUID businessId, UUID serviceId, UpdateServiceRequest request, boolean requireCategory) {
        ServiceEntity service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + serviceId));

        if (requireCategory && request.categoryId() == null) {
            throw new ValidationException("A categoria do serviço é obrigatória.");
        }

        if (request.categoryId() != null) {
            CategoryEntity category = categoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoria não encontrada ou inativa: " + request.categoryId()));
            service.setCategory(category);
        }

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

        if (request.imageUrl() != null) {
            service.setImageUrl(serviceMapper.normalizeUrl(request.imageUrl()));
        }

        if (request.isActive() != null) {
            service.setActive(request.isActive());
        }

        ServiceEntity updated = serviceRepository.save(service);
        List<UUID> assignedEmployeeIds = serviceAssignmentLookupService.findAssignedEmployeeIds(businessId, serviceId);
        return serviceMapper.toResponse(updated, assignedEmployeeIds);
    }
}
