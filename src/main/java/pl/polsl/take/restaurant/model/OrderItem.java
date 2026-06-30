package pl.polsl.take.restaurant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;

@Entity
@Getter
@NoArgsConstructor
public class OrderItem
 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", updatable = false) // cannot change dish in order item - item should be cancelled and reordered instead
    private Dish dish;

    @Column(updatable = false)
    private Integer dishPriceAtOrderTime;   //save dish price at order

    @Column(updatable = false)
    private Integer quantity;   //usually 1

    private Integer seatNumber;

    private String notes;

    @Setter
    @Getter
    private Boolean isCancelled = false;

    @Setter
    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;

    //constructor for order item (not every field has setter)
    public OrderItem(Dish dish, Integer quantity, Integer seatNumber, String notes, Order order) {
        this.dish = dish;
        this.dishPriceAtOrderTime = dish.getPriceInCents();
        this.quantity = quantity;
        this.seatNumber = seatNumber;
        this.notes = notes;
        this.order = order;
        this.status = OrderItemStatus.NEW; // default status
    }
}
