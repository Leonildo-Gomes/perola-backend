package cv.perola.modules.business.usecase.business;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessCategoryEntity;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.domain.CategoryEntity;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.dto.UpdateBusinessRequest;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.mapper.CategoryMapper;
import cv.perola.modules.business.repository.BusinessCategoryRepository;
import cv.perola.modules.business.repository.BusinessRepository;
import cv.perola.modules.business.validator.business.BusinessCategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateBusinessUseCase {

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository businessCategoryRepository;
    private final BusinessMapper businessMapper;
    private final CategoryMapper categoryMapper;
    private final BusinessCategoryValidator businessCategoryValidator;

    @Transactional
    public BusinessResponse execute(UUID businessId, UpdateBusinessRequest request) {
        if (businessId == null) {
            throw new ResourceNotFoundException("Identificador de estabelecimento inválido.");
        }

        BusinessEntity business = businessRepository.findByIdAndIsActiveTrue(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado ou inativo."));

        if (request != null) {
            if (request.name() != null && !request.name().isBlank()) {
                business.setName(request.name().trim());
            }
            if (request.island() != null && !request.island().isBlank()) {
                business.setIsland(request.island().trim());
            }
            if (request.city() != null && !request.city().isBlank()) {
                business.setCity(request.city().trim());
            }
            if (request.location() != null && !request.location().isBlank()) {
                business.setLocation(request.location().trim());
            }
            if (request.contactPhone() != null && !request.contactPhone().isBlank()) {
                business.setContactPhone(request.contactPhone().trim());
            }
            if (request.timezone() != null && !request.timezone().isBlank()) {
                business.setTimezone(request.timezone().trim());
            }
        }

        BusinessEntity updated = businessRepository.save(business);

        List<BusinessCategoryEntity> categories;
        if (request != null && request.categories() != null) {
            Map<UUID, CategoryEntity> categoryMap = businessCategoryValidator.validateAndResolveCategories(request.categories());
            businessCategoryRepository.deleteAllByBusinessId(businessId);
            businessCategoryRepository.flush();
            List<BusinessCategoryEntity> newCategories = categoryMapper.toBusinessCategoryEntities(
                    businessId,
                    request.categories(),
                    categoryMap
            );
            categories = businessCategoryRepository.saveAll(newCategories);
        } else {
            categories = businessCategoryRepository.findAllByBusinessId(businessId);
        }

        List<BusinessCategoryResponse> categoryResponses = categoryMapper.toBusinessCategoryResponses(categories);
        return businessMapper.toResponse(updated, categoryResponses);
    }
}
