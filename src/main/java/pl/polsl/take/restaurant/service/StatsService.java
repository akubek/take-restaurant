package pl.polsl.take.restaurant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.DishPopularityDTO;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.OrderItemRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

/**
 * Provides reporting use-cases such as revenue and dish popularity.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final DishRepository dishRepo;

    /**
     * Returns total paid revenue in the provided date-time range.
     *
     * @param from range start
     * @param to range end
     * @return revenue in minor currency units
     */
    @Transactional(readOnly = true)
    public Long getRevenueBetween(LocalDateTime from, LocalDateTime to) {
        return orderRepo.sumRevenueBetween(from, to).orElse(0L);
    }

    /**
     * Returns revenue for current day.
     *
     * @return revenue in minor currency units
     */
    @Transactional(readOnly = true)
    public Long getTodayRevenue() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return getRevenueBetween(start, end);
    }

    /**
     * Returns revenue for trailing 7-day window.
     *
     * @return revenue in minor currency units
     */
    @Transactional(readOnly = true)
    public Long getWeekRevenue() {
        LocalDateTime start = LocalDate.now().minusWeeks(1).atStartOfDay();
        return getRevenueBetween(start, LocalDateTime.now());
    }

    /**
     * Returns revenue for trailing 1-month window.
     *
     * @return revenue in minor currency units
     */
    @Transactional(readOnly = true)
    public Long getMonthRevenue() {
        LocalDateTime start = LocalDate.now().minusMonths(1).atStartOfDay();
        return getRevenueBetween(start, LocalDateTime.now());
    }


    /**
     * Returns dish popularity as pairs of dish ID and total sold quantity for a date-time range.
     *
     * @param from range start
     * @param to range end
     * @return list of popularity DTOs
     */
    @Transactional(readOnly = true)
    public List<DishPopularityDTO> getDishPopularity(LocalDateTime from, LocalDateTime to) {
        List<Object[]> results = orderItemRepo.countDishOrders(from, to);

        return results.stream().map(row -> {
            Long dishId = ((Number) row[0]).longValue();
            Long totalSold = ((Number) row[1]).longValue();

            String dishName = dishRepo.findById(dishId)
                    .map(dish -> dish.getName())
                    .orElse("Unknown Dish");

            return new DishPopularityDTO(dishId, dishName, totalSold);
        }).toList();
    }
}