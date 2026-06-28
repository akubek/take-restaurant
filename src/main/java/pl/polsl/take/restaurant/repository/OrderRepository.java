package pl.polsl.take.restaurant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.polsl.take.restaurant.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByOrderDateTimeBetween(LocalDateTime from, LocalDateTime to);
}

