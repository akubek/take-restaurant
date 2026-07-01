package pl.polsl.take.restaurant.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.OrderStatus;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Order entity representing either a live table order or a future reservation.
 */
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", updatable = false)
    private Customer customer;

    /**
     * Creation time for walk-in orders or scheduled date-time for reservations.
     */
    @Column(updatable = false, nullable = false)
    @NotNull
    private LocalDateTime orderDateTime;

    @Setter
    private Integer tableNumber;

    @Setter
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, name = "order_status")
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Creates an order for current time.
     *
     * @param customer customer owning the order (nullable for anonymous order)
     * @param tableNumber assigned table number
     */
    public Order(Customer customer, Integer tableNumber) {
        this.customer = customer;
        this.tableNumber = tableNumber;
        this.orderDateTime = LocalDateTime.now();
        this.status = OrderStatus.OPEN;
    }

    /**
     * Creates an order scheduled for a specific date-time (reservation).
     *
     * @param customer customer owning the reservation
     * @param tableNumber reserved table number
     * @param orderDateTime reservation date-time
     */
    public Order(Customer customer, Integer tableNumber, LocalDateTime orderDateTime) {
        this.customer = customer;
        this.tableNumber = tableNumber;
        this.orderDateTime = orderDateTime;
        this.status = OrderStatus.OPEN;
    }
}
