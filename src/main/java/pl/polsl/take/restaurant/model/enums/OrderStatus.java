package pl.polsl.take.restaurant.model.enums;

/**
 * Lifecycle status of the whole order.
 */
public enum OrderStatus {
    /** Order is active and can still be processed. */
    OPEN,
    /** Order has been fully paid. */
    PAID,
    /** Order has been cancelled and should not be processed further. */
    CANCELLED
}
