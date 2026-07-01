package pl.polsl.take.restaurant.exception;

/**
 * Thrown when a requested entity does not exist or is not accessible.
 */
public class NotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception with a user-facing message.
     *
     * @param message not-found details
     */
    public NotFoundException(String message) {
        super(message);
    }
}
