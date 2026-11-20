package cv.perola.core.exception;

public class PendingAppointmentsLimitExceededException extends RuntimeException {
    public PendingAppointmentsLimitExceededException(String message) {
        super(message);
    }
}
