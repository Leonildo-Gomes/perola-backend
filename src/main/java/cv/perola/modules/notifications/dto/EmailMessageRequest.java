package cv.perola.modules.notifications.dto;

import java.util.Map;

import cv.perola.modules.notifications.domain.enums.EmailTemplate;
import lombok.Builder;

@Builder
public record EmailMessageRequest(
                String to,
                String recipientName,
                String subject,
                EmailTemplate template,
                Map<String, Object> variables) {

}
