package pl.polsl.take.restaurant.exception;

/**
 * Thrown when a request cannot be completed due to current business state or resource conflict.
 */
public class ConflictException extends RuntimeException {

    /**
     * Creates a conflict exception with a user-facing message.
     *
     * @param message conflict details
     */
    public ConflictException(String message) {
        super(message);
    }
}
