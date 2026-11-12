package cv.perola.modules.business.dto;

import org.springframework.core.io.Resource;

public record MediaResourceResult(
        Resource resource,
        String contentType
) {
}

