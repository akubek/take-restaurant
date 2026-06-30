package pl.polsl.take.restaurant.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepo;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private DishRepository dishRepo;

    @Autowired
    private CustomerRepository customerRepo;

    private Customer customer;
    private Dish pizza;
    private Dish burger;

    @BeforeEach
    void setUp() {
        // Czyszczenie bazy przed każdym testem (ważne dla powtarzalności)
        orderRepo.deleteAll();
        dishRepo.deleteAll();
        customerRepo.deleteAll();

        // Przygotowanie wspólnych danych
        customer = customerRepo.save(new Customer("Jan", "Kowalski", "123", "test@test.com"));
        pizza = dishRepo.save(new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD));
        burger = dishRepo.save(new Dish("Burger", "Opis", 2500, 800, SpicinessLevel.MILD));
    }

    @Test
    void countDishOrders_shouldAggregateQuantitiesCorrectly() {
        // Given - tworzymy zamówienia w konkretnych datach
        LocalDateTime now = LocalDateTime.now();
        
        // Zamówienie 1 (Dzisiaj): 2 Pizze, 1 Burger
        Order order1 = new Order(customer, 1, now);
        order1.getOrderItems().add(new OrderItem(pizza, 2, 1, null, order1));
        order1.getOrderItems().add(new OrderItem(burger, 1, 2, null, order1));
        orderRepo.save(order1);

        // Zamówienie 2 (Dzisiaj): 3 Pizze
        Order order2 = new Order(customer, 2, now.plusHours(1));
        order2.getOrderItems().add(new OrderItem(pizza, 3, 1, null, order2));
        orderRepo.save(order2);

        // Zamówienie 3 (Poza zakresem - wczoraj): 5 Burgerów -> nie powinno wejść do wyniku!
        Order order3 = new Order(customer, 3, now.minusDays(1));
        order3.getOrderItems().add(new OrderItem(burger, 5, 1, null, order3));
        orderRepo.save(order3);

        LocalDateTime from = now.minusMinutes(10);
        LocalDateTime to = now.plusHours(2);

        // When - pobieramy statystyki tylko dla "dzisiaj"
        List<Object[]> results = orderItemRepo.countDishOrders(from, to);

        // Then
        // Oczekujemy 2 wyników (Pizza i Burger)
        assertEquals(2, results.size(), "Powinny zostać zwrócone dwa różne dania");

        // Szukamy wyników w tablicy (kolejność może być różna w zależności od bazy, więc szukamy po ID)
        boolean pizzaFound = false;
        boolean burgerFound = false;

        for (Object[] row : results) {
            Long dishId = ((Number) row[0]).longValue();
            Long totalQuantity = ((Number) row[1]).longValue();

            if (dishId.equals(pizza.getId())) {
                assertEquals(5L, totalQuantity, "Suma pizz powinna wynosić 5 (2 + 3)");
                pizzaFound = true;
            } else if (dishId.equals(burger.getId())) {
                assertEquals(1L, totalQuantity, "Suma burgerów powinna wynosić 1 (zamówienie z wczoraj jest ignorowane)");
                burgerFound = true;
            }
        }

        assertTrue(pizzaFound, "Pizza powinna być w wynikach");
        assertTrue(burgerFound, "Burger powinien być w wynikach");
    }

    @Test
    void countDishOrders_shouldReturnEmptyListWhenNoOrdersInRange() {
        // Given - tylko jedno zamówienie rok temu
        Order order = new Order(customer, 1, LocalDateTime.now().minusYears(1));
        order.getOrderItems().add(new OrderItem(pizza, 10, 1, null, order));
        orderRepo.save(order);

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        // When
        List<Object[]> results = orderItemRepo.countDishOrders(from, to);

        // Then
        assertTrue(results.isEmpty(), "Wynik powinien być pusty, jeśli nie ma zamówień w podanym przedziale czasowym");
    }
}