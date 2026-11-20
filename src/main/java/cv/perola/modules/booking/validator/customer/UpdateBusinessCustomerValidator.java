package cv.perola.modules.booking.validator.customer;

import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.dto.UpdateBusinessCustomerRequest;
import org.springframework.stereotype.Component;

@Component
public class UpdateBusinessCustomerValidator {

    public void validate(UpdateBusinessCustomerRequest request) {
        if (request == null || (request.tier() == null && request.internalNotes() == null)) {
            throw new ValidationException(
                    "Deve fornecer pelo menos a classificação (tier) ou as notas internas para atualização.");
        }
    }
}
