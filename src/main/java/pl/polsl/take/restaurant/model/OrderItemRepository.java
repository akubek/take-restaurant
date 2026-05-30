package pl.polsl.take.restaurant.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        SELECT oi.dish.id, SUM(oi.quantity)
        FROM OrderItem oi
        WHERE oi.order.orderDateTime BETWEEN :from AND :to
        GROUP BY oi.dish.id
    """)
    List<Object[]> countDishOrders(LocalDateTime from, LocalDateTime to);
}