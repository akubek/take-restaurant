package pl.polsl.take.restaurant.exception;

/**
 * Standard API error payload returned by exception handlers.
 *
 * @param status HTTP status code
 * @param message human-readable error message
 */
public record ErrorResponse(int status, String message) {
}
