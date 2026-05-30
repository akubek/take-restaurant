package pl.polsl.take.restaurant;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.model.OrderRepository;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final OrderRepository repo;

    public int todayRevenue() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return sum(start, LocalDateTime.now());
    }

    public int weekRevenue() {
        LocalDateTime start = LocalDate.now().minusDays(7).atStartOfDay();
        return sum(start, LocalDateTime.now());
    }

    private int sum(LocalDateTime from, LocalDateTime to) {
        return repo.findByOrderDateTimeBetween(from, to)
                .stream()
                .flatMap(o -> o.getOrderItems().stream())
                .mapToInt(i -> i.getDishPriceAtOrderTime() * i.getQuantity())
                .sum();
    }
}