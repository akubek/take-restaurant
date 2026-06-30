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
class StatsServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private OrderItemRepository orderItemRepo;

    @Mock
    private DishRepository dishRepo;

    @InjectMocks
    private StatsService statsService;

    // -------------------------------------------------------------------------
    // getRevenueBetween
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnRevenueForGivenDateRange() {
        // Given
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 6, 30, 23, 59);
        when(orderRepo.sumRevenueBetween(from, to)).thenReturn(Optional.of(15000L));

        // When
        Long result = statsService.getRevenueBetween(from, to);

        // Then
        assertEquals(15000L, result);
        verify(orderRepo).sumRevenueBetween(from, to);
    }

    @Test
    void shouldReturnZeroWhenNoOrdersInDateRange() {
        // Given - brak zamówień → repozytorium zwraca Optional.empty()
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(orderRepo.sumRevenueBetween(from, to)).thenReturn(Optional.empty());

        // When
        Long result = statsService.getRevenueBetween(from, to);

        // Then
        assertEquals(0L, result);
    }

    // -------------------------------------------------------------------------
    // getTodayRevenue
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnTodayRevenueByDelegatingToSumRevenueBetween() {
        // Given - mockujemy sumRevenueBetween dla dowolnego zakresu dat (dzisiejszy dzień)
        when(orderRepo.sumRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(3200L));

        // When
        Long result = statsService.getTodayRevenue();

        // Then
        assertEquals(3200L, result);
        verify(orderRepo).sumRevenueBetween(any(), any());
    }

    @Test
    void shouldReturnZeroForTodayWhenNoOrders() {
        when(orderRepo.sumRevenueBetween(any(), any())).thenReturn(Optional.empty());

        Long result = statsService.getTodayRevenue();

        assertEquals(0L, result);
    }

    // -------------------------------------------------------------------------
    // getWeekRevenue
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getMonthRevenue
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getDishPopularity
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnDishPopularityWithNames() {
        // Given
        Dish pizza = new Dish("Pizza Margherita", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(pizza, "id", 1L);

        Dish burger = new Dish("Burger Klasyczny", "Opis", 2500, 700, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(burger, "id", 2L);

        // Symulacja wyników z JPQL: [dishId, totalSold]
        Object[] row1 = new Object[]{1L, 15L};
        Object[] row2 = new Object[]{2L, 8L};

        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.of(row1, row2));
        when(dishRepo.findById(1L)).thenReturn(Optional.of(pizza));
        when(dishRepo.findById(2L)).thenReturn(Optional.of(burger));

        // When
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then
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
        // Given - danie zostało usunięte z bazy, ale figuruje w historii zamówień
        Object[] row = new Object[]{99L, 3L};

        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 1, 31, 23, 59);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.<Object[]>of(row));
        when(dishRepo.findById(99L)).thenReturn(Optional.empty());

        // When
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then - fallback na "Unknown Dish"
        assertEquals(1, result.size());
        assertEquals("Unknown Dish", result.get(0).getDishName());
        assertEquals(99L, result.get(0).getDishId());
        assertEquals(3L, result.get(0).getTotalSold());
    }

    @Test
    void shouldReturnEmptyListWhenNoDishesOrderedInRange() {
        // Given - brak zamówień w podanym przedziale
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 5, 31, 23, 59);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.of());

        // When
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then
        assertTrue(result.isEmpty());
        verify(dishRepo, never()).findById(any()); // nie wchodzi do pętli mapowania
    }

    @Test
    void shouldHandleIntegerRowValuesFromJpqlAggregation() {
        // Given - JPQL może zwrócić Integer zamiast Long w zależności od bazy danych
        Object[] row = new Object[]{Integer.valueOf(5), Integer.valueOf(10)};

        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 6, 30, 23, 59);

        Dish dish = new Dish("Zupa", "Opis", 1500, 300, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 5L);

        when(orderItemRepo.countDishOrders(from, to)).thenReturn(List.<Object[]>of(row));
        when(dishRepo.findById(5L)).thenReturn(Optional.of(dish));

        // When
        List<DishPopularityDTO> result = statsService.getDishPopularity(from, to);

        // Then - konwersja Number → Long działa poprawnie
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getDishId());
        assertEquals(10L, result.get(0).getTotalSold());
    }
}
