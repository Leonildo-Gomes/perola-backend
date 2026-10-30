package cv.perola.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    public ProblemDetail handleBusinessValidationException(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Os dados fornecidos contêm erros de validação."
        );
        problem.setTitle("Erro de Validação");
        problem.setType(URI.create("https://perola.cv/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleRequestParameterException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Os parâmetros de consulta fornecidos são inválidos ou estão ausentes."
        );
        problem.setTitle("Erro de Validação");
        problem.setType(URI.create("https://perola.cv/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
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

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        String detail = "Conflito de integridade de dados.";
        if (ex.getMessage() != null && ex.getMessage().contains("uk_businesses_owner_id")) {
            detail = "O utilizador já possui um salão registado. No MVP é permitido apenas 1 negócio por proprietário.";
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                detail
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

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ProblemDetail handleTenantAccessDeniedException(TenantAccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage() != null ? ex.getMessage() : "Acesso não autorizado para o salão especificado."
        );
        problem.setTitle("Acesso ao Salão Negado");
        problem.setType(URI.create("https://perola.cv/errors/tenant-access-denied"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ProblemDetail handleInvalidApiVersionException(InvalidApiVersionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Versão da API inválida ou não suportada."
        );
        problem.setTitle("Versão de API Inválida");
        problem.setType(URI.create("https://perola.cv/errors/invalid-api-version"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingApiVersionException.class)
    public ProblemDetail handleMissingApiVersionException(MissingApiVersionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "A versão da API é obrigatória na requisição."
        );
        problem.setTitle("Versão de API Ausente");
        problem.setType(URI.create("https://perola.cv/errors/missing-api-version"));
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
