package cv.perola.core.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve mapear violação da constraint GiST ex_appointments_no_overlap (SQLState 23P01) para HTTP 409 slot-already-booked")
    void shouldMapGistOverlapViolationToSlotAlreadyBookedProblemDetail() {
        // Arrange
        SQLException psqlException = new SQLException(
                "ERROR: conflicting key value violates exclusion constraint \"ex_appointments_no_overlap\"",
                "23P01"
        );
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement", psqlException
        );

        // Act
        ProblemDetail problem = globalExceptionHandler.handleDataIntegrityViolationException(ex);

        // Assert
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getType()).isEqualTo(URI.create("https://perola.cv/errors/slot-already-booked"));
        assertThat(problem.getTitle()).isEqualTo("Conflito de Horário");
        assertThat(problem.getDetail()).isEqualTo(
                "O profissional selecionado já possui um agendamento confirmado ou pendente neste intervalo de tempo."
        );
    }

    @Test
    @DisplayName("Deve mapear violação de uk_businesses_owner_id para HTTP 409 conflict genérico")
    void shouldMapBusinessOwnerUniqueConstraintToConflictProblemDetail() {
        // Arrange
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_businesses_owner_id\""
        );

        // Act
        ProblemDetail problem = globalExceptionHandler.handleDataIntegrityViolationException(ex);

        // Assert
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getType()).isEqualTo(URI.create("https://perola.cv/errors/conflict"));
        assertThat(problem.getTitle()).isEqualTo("Conflito de Dados");
        assertThat(problem.getDetail()).contains("1 negócio por proprietário");
    }
}

