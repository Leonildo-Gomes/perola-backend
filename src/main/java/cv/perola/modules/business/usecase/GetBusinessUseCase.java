package cv.perola.modules.business.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.BusinessEntity;
import cv.perola.modules.business.dto.BusinessResponse;
import cv.perola.modules.business.mapper.BusinessMapper;
import cv.perola.modules.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetBusinessUseCase {

    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;

    @Transactional(readOnly = true)
    public BusinessResponse execute(UUID businessId) {
        if (businessId == null) {
            throw new ResourceNotFoundException("Identificador de estabelecimento inválido.");
        }

        BusinessEntity business = businessRepository.findByIdAndIsActiveTrue(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado ou inativo."));

        return businessMapper.toResponse(business);
    }
}
