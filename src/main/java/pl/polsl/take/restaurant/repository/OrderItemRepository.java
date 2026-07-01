package pl.polsl.take.restaurant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.polsl.take.restaurant.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Returns rows of [dishId, totalSold] for non-cancelled order items in the date-time range.
     */
    @Query("""
                SELECT oi.dish.id, SUM(oi.quantity)
                FROM OrderItem oi
                WHERE oi.order.orderDateTime BETWEEN :from AND :to
                AND oi.isCancelled = false
                GROUP BY oi.dish.id
                ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> countDishOrders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}