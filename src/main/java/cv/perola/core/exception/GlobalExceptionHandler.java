package cv.perola.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Os dados fornecidos contêm erros de validação."
        );
        problem.setTitle("Erro de Validação");
        problem.setType(URI.create("https://perola.cv/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());

        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        problem.setProperty("invalidParams", invalidParams);
        return problem;
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationFailedException.class})
    public ProblemDetail handleAuthenticationException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage() != null ? ex.getMessage() : "Email ou palavra-passe incorretos."
        );
        problem.setTitle("Credenciais Inválidas");
        problem.setType(URI.create("https://perola.cv/errors/invalid-credentials"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidTokenException(InvalidTokenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problem.setTitle("Token Inválido");
        problem.setType(URI.create("https://perola.cv/errors/invalid-token"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabledException(DisabledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "A sua conta encontra-se desativada."
        );
        problem.setTitle("Conta Desativada");
        problem.setType(URI.create("https://perola.cv/errors/account-disabled"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflito de Dados");
        problem.setType(URI.create("https://perola.cv/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://perola.cv/errors/resource-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Não tem permissão para aceder a este recurso."
        );
        problem.setTitle("Acesso Não Autorizado");
        problem.setType(URI.create("https://perola.cv/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://perola.cv/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private Map<String, String> formatFieldError(FieldError error) {
        return Map.of(
                "name", error.getField(),
                "reason", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido"
        );
    }
}
