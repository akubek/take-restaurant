package pl.polsl.take.restaurant.model.enums;

/**
 * Processing state of a single order item in the kitchen workflow.
 */
public enum OrderItemStatus {
    /** Item was accepted and is waiting to be prepared. */
    NEW,
    /** Item is currently being prepared. */
    PREPARING,
    /** Item is ready for pickup/service. */
    READY,
    /** Item was delivered to the customer. */
    DELIVERED
}
