package pl.polsl.take.restaurant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.polsl.take.restaurant.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        SELECT oi.dish.id, SUM(oi.quantity)
        FROM OrderItem oi
        WHERE oi.order.orderDateTime BETWEEN :from AND :to
        GROUP BY oi.dish.id
    """)
    List<Object[]> countDishOrders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}