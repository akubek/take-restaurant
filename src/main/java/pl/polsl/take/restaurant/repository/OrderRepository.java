package pl.polsl.take.restaurant.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.enums.OrderStatus;


public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByOrderDateTimeBetween(LocalDateTime from, LocalDateTime to);

    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusAndOrderDateTimeBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);
    boolean existsByOrderItemsDishId(Long dishId);

    // get client spending
    @Query("SELECT SUM(oi.dishPriceAtOrderTime * oi.quantity) FROM Order o " +
        "JOIN o.orderItems oi WHERE o.customer.id = :customerId " +
        "AND o.status = pl.polsl.take.restaurant.model.enums.OrderStatus.PAID")
    Optional<Long> sumCustomerSpending(@Param("customerId") Long customerId);

    // get revenue between two dates
    @Query("SELECT SUM(oi.dishPriceAtOrderTime * oi.quantity) FROM Order o " +
        "JOIN o.orderItems oi WHERE o.orderDateTime BETWEEN :from AND :to " +
        "AND o.status = pl.polsl.take.restaurant.model.enums.OrderStatus.PAID")
    Optional<Long> sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

