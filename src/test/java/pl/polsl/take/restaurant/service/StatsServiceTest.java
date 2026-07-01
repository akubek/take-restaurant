package pl.polsl.take.restaurant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pl.polsl.take.restaurant.dto.DishPopularityDTO;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.OrderItemRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for statistics service calculations and mapping of popularity query results.
 */
class StatsServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private OrderItemRepository orderItemRepo;

    @Mock
    private DishRepository dishRepo;

    @InjectMocks
    private StatsService statsService;

    @Test
    void shouldReturnRevenueForGivenDateRange() {
        // Given: repository returns aggregated revenue for selected range
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 30, 23, 59);
        when(orderRepo.sumRevenueBetween(from, to)).thenReturn(Optional.of(15000L));

        // When: revenue is requested
        Long result = statsService.getRevenueBetween(from, to);

        // Then: returned value matches repository aggregation
        assertEquals(15000L, result);
        verify(orderRepo).sumRevenueBetween(from, to);
    }

    @Test
    void shouldReturnZeroWhenNoOrdersInDateRange() {
        // Given: repository returns empty aggregate
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(orderRepo.sumRevenueBetween(from, to)).thenReturn(Optional.empty());

        // When: revenue is requested
        Long result = statsService.getRevenueBetween(from, to);

        // Then: service maps empty aggregate to zero
        assertEquals(0L, result);
    }

    @Test
    void shouldReturnTodayRevenueByDelegatingToSumRevenueBetween() {

        when(orderRepo.sumRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(3200L));

        Long result = statsService.getTodayRevenue();

        assertEquals(3200L, result);
        verify(orderRepo).sumRevenueBetween(any(), any());
    }

    @Test
    void shouldReturnZeroForTodayWhenNoOrders() {
        when(orderRepo.sumRevenueBetween(any(), any())).thenReturn(Optional.empty());

        Long result = statsService.getTodayRevenue();

        assertEquals(0L, result);
    }

    @Test
    void shouldReturnWeekRevenue() {
        when(orderRepo.sumRevenueBetween(any(), any())).thenReturn(Optional.of(42000L));

        Long result = statsService.getWeekRevenue();

        assertEquals(42000L, result);
    }

    @Test
    void shouldReturnZeroForWeekWhenNoOrders() {
        when(orderRepo.sumRevenueBetween(any(), any())).thenReturn(Optional.empty());

        assertEquals(0L, statsService.getWeekRevenue());
    }

    @Test
    void shouldReturnMonthRevenue() {
        when(orderRepo.sumRevenueBetween(any(), any())).thenReturn(Optional.of(150000L));

        Long result = statsService.getMonthRevenue();

        assertEquals(150000L, result);
    }

    @Test
    void shouldReturnZeroForMonthWhenNoOrders() {
        when(orderRepo.sumRevenueBetween(any(), any())).thenReturn(Optional.empty());

        assertEquals(0L, statsService.getMonthRevenue());
    }

    @Test
    void shouldReturnDishPopularityWithNames() {
        // Given: popularity rows and existing dishes for each ID
        Dish pizza = new Dish("Pizza Margherita", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(pizza, "id", 1L);

        Dish burger = new Dish("Burger Klasyczny", "Opis", 2500, 700, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(burger, "id", 2L);

        Object[] row1 = new Object[] { 1L, 15L };
        Object[] row2 = new Object[] { 2L, 8L };

        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.of(row1, row2));
        when(dishRepo.findById(1L)).thenReturn(Optional.of(pizza));
        when(dishRepo.findById(2L)).thenReturn(Optional.of(burger));

        // When: popularity report is requested
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then: IDs, names and totals are mapped correctly
        assertEquals(2, result.size());

        DishPopularityDTO first = result.get(0);
        assertEquals(1L, first.getDishId());
        assertEquals("Pizza Margherita", first.getDishName());
        assertEquals(15L, first.getTotalSold());

        DishPopularityDTO second = result.get(1);
        assertEquals(2L, second.getDishId());
        assertEquals("Burger Klasyczny", second.getDishName());
        assertEquals(8L, second.getTotalSold());
    }

    @Test
    void shouldReturnUnknownDishWhenDishNoLongerExistsInDatabase() {
        // Given: popularity row references dish missing in repository
        Object[] row = new Object[] { 99L, 3L };

        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.<Object[]>of(row));
        when(dishRepo.findById(99L)).thenReturn(Optional.empty());

        // When: popularity report is requested
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then: missing dish name is replaced with fallback value
        assertEquals(1, result.size());
        assertEquals("Unknown Dish", result.get(0).getDishName());
        assertEquals(99L, result.get(0).getDishId());
        assertEquals(3L, result.get(0).getTotalSold());
    }

    @Test
    void shouldReturnEmptyListWhenNoDishesOrderedInRange() {

        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 31, 23, 59);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.of());

        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        assertTrue(result.isEmpty());
        verify(dishRepo, never()).findById(any());
    }

    @Test
    void shouldHandleIntegerRowValuesFromJpqlAggregation() {
        // Given: aggregation row returned as Integer values by JPA provider
        Object[] row = new Object[] { Integer.valueOf(5), Integer.valueOf(10) };

        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 30, 23, 59);

        Dish dish = new Dish("Zupa", "Opis", 1500, 300, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 5L);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.<Object[]>of(row));
        when(dishRepo.findById(5L)).thenReturn(Optional.of(dish));

        // When: popularity report is requested
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then: numeric values are normalized to Long in DTO
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getDishId());
        assertEquals(10L, result.get(0).getTotalSold());
    }
}
