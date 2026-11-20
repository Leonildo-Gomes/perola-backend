package cv.perola.modules.booking.validator.customer;

import cv.perola.core.exception.ValidationException;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import cv.perola.modules.booking.dto.UpdateBusinessCustomerRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UpdateBusinessCustomerValidatorTest {

    private final UpdateBusinessCustomerValidator validator = new UpdateBusinessCustomerValidator();

    @Test
    @DisplayName("Deve validar com sucesso quando apenas o tier for fornecido")
    void shouldValidateSuccessfullyWhenOnlyTierProvided() {
        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(CustomerTier.VIP, null);
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Deve validar com sucesso quando apenas notas internas forem fornecidas")
    void shouldValidateSuccessfullyWhenOnlyNotesProvided() {
        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(null, "Prefere atendimento matinal.");
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Deve validar com sucesso quando ambos tier e notas forem fornecidos")
    void shouldValidateSuccessfullyWhenBothTierAndNotesProvided() {
        UpdateBusinessCustomerRequest request = new UpdateBusinessCustomerRequest(CustomerTier.RECURRENT, "Cliente habitual.");
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o request for nulo ou ambos os campos forem nulos")
    void shouldThrowValidationExceptionWhenBothFieldsAreNull() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Deve fornecer pelo menos a classificação");

        UpdateBusinessCustomerRequest emptyRequest = new UpdateBusinessCustomerRequest(null, null);
        assertThatThrownBy(() -> validator.validate(emptyRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Deve fornecer pelo menos a classificação");
    }
}
