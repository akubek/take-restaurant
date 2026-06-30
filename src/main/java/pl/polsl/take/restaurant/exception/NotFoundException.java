package pl.polsl.take.restaurant.exception;
 
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
