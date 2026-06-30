package pl.polsl.take.restaurant.exception;
 
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
