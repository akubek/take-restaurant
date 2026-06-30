package pl.polsl.take.restaurant.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;
import pl.polsl.take.restaurant.model.enums.OrderStatus;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// @DataJpaTest podnosi H2 w pamięci, ładuje tylko warstwę JPA (bez kontrolerów, bez serwisów)
// każdy test jest domyślnie otoczony transakcją która jest rollbackowana po teście
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DishRepository dishRepository;

    // encje wspólne dla testów
    private Customer customer;
    private Dish dish;

    @BeforeEach
    void setUp() {
        // kolejność ważna ze względu na FK constraints:
        // najpierw encje zależne (orders → order items przez CASCADE)
        // potem encje bazowe
        orderRepository.deleteAll();   // kaskadowo usuwa też order items
        dishRepository.deleteAll();    // kaskadowo usuwa też recipe items
        customerRepository.deleteAll();

        customer = customerRepository.save(
                new Customer("Jan", "Kowalski", "123456789", "jan@test.com"));
        dish = dishRepository.save(
                new Dish("Pizza Margherita", "Klasyczna pizza", 3200, 800, SpicinessLevel.MILD));
    }

    // -------------------------------------------------------------------------
    // sumRevenueBetween
    // -------------------------------------------------------------------------

    @Test
    void sumRevenueBetween_shouldReturnCorrectSumForPaidOrders() {
        // Given - zamówienie PAID z 2 pozycjami tej samej pizzy
        createPaidOrderWithItem(dish, 2);

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        // When
        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        // Then - 2 * 3200 = 6400
        assertTrue(revenue.isPresent());
        assertEquals(6400L, revenue.get());
    }

    @Test
    void sumRevenueBetween_shouldReturnEmptyWhenNoOrdersInRange() {
        // Given - zamówienia w przeszłości, ale zapytanie na zakres który ich nie obejmuje
        // (brak zamówień w bazie w ogóle - setUp nie tworzy zamówień)
        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now().minusDays(1);

        // When
        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        // Then - SQL SUM na pustym zbiorze → NULL → Optional.empty()
        assertTrue(revenue.isEmpty());
    }

    @Test
    void sumRevenueBetween_shouldIgnoreCancelledOrders() {
        // Given - zamówienie CANCELLED - nie powinno wchodzić do raportu
        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 3, 1, null, order);
        order.getOrderItems().add(item);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        // When
        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        // Then - CANCELLED ignorowane
        assertTrue(revenue.isEmpty());
    }

    @Test
    void sumRevenueBetween_shouldIgnoreCancelledItemsInPaidOrder() {
        // Given - zamówienie PAID z jedną ważną pozycją i jedną anulowaną
        Order order = new Order(customer, 1);

        OrderItem validItem = new OrderItem(dish, 1, 1, null, order);  // 3200
        OrderItem cancelledItem = new OrderItem(dish, 5, 2, null, order);  // 5*3200 = 16000 - NIE liczyć
        cancelledItem.setIsCancelled(true);

        order.getOrderItems().addAll(List.of(validItem, cancelledItem));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        // When
        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        // Then - tylko validItem: 1 * 3200 = 3200
        assertTrue(revenue.isPresent());
        assertEquals(3200L, revenue.get());
    }

    // -------------------------------------------------------------------------
    // sumCustomerSpending
    // -------------------------------------------------------------------------

    @Test
    void sumCustomerSpending_shouldReturnTotalForCustomer() {
        // Given - dwa zamówienia tego samego klienta
        createPaidOrderWithItem(dish, 1); // 3200
        createPaidOrderWithItem(dish, 2); // 6400

        // When
        Optional<Long> spending = orderRepository.sumCustomerSpending(customer.getId());

        // Then - łącznie 9600
        assertTrue(spending.isPresent());
        assertEquals(9600L, spending.get());
    }

    @Test
    void sumCustomerSpending_shouldReturnEmptyForCustomerWithNoOrders() {
        // Given - klient bez zamówień
        Customer newCustomer = customerRepository.save(
                new Customer("Anna", "Nowak", null, null));

        // When
        Optional<Long> spending = orderRepository.sumCustomerSpending(newCustomer.getId());

        // Then
        assertTrue(spending.isEmpty());
    }

    @Test
    void sumCustomerSpending_shouldNotCountOpenOrders() {
        // Given - zamówienie OPEN (jeszcze nieopłacone)
        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        order.getOrderItems().add(item);
        // status pozostaje OPEN (domyślny)
        orderRepository.save(order);

        // When
        Optional<Long> spending = orderRepository.sumCustomerSpending(customer.getId());

        // Then - OPEN nie liczy się do wydatków
        assertTrue(spending.isEmpty());
    }

    // -------------------------------------------------------------------------
    // existsByTableNumberAndStatusNotAndOrderDateTimeBetween (dostępność stolika)
    // -------------------------------------------------------------------------

    @Test
    void tableAvailability_shouldReturnTrueWhenTableOccupied() {
        // Given - rezerwacja stolika nr 5 na jutro
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Order reservation = new Order(customer, 5, tomorrow);
        // status OPEN (nie CANCELLED) - liczy się jako zajęty
        orderRepository.save(reservation);

        LocalDateTime checkFrom = tomorrow.minusHours(1);
        LocalDateTime checkTo = tomorrow.plusHours(1);

        // When
        boolean occupied = orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                5, OrderStatus.CANCELLED, checkFrom, checkTo);

        // Then
        assertTrue(occupied);
    }

    @Test
    void tableAvailability_shouldReturnFalseForCancelledReservation() {
        // Given - rezerwacja anulowana - stolik powinien być dostępny
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Order reservation = new Order(customer, 5, tomorrow);
        reservation.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(reservation);

        LocalDateTime checkFrom = tomorrow.minusHours(1);
        LocalDateTime checkTo = tomorrow.plusHours(1);

        // When - sprawdzamy "status != CANCELLED", więc CANCELLED jest ignorowany
        boolean occupied = orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                5, OrderStatus.CANCELLED, checkFrom, checkTo);

        // Then - anulowana rezerwacja nie blokuje stolika
        assertFalse(occupied);
    }

    @Test
    void tableAvailability_shouldReturnFalseForDifferentTable() {
        // Given - rezerwacja na stołek nr 5, ale sprawdzamy nr 3
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Order reservation = new Order(customer, 5, tomorrow);
        orderRepository.save(reservation);

        LocalDateTime checkFrom = tomorrow.minusHours(1);
        LocalDateTime checkTo = tomorrow.plusHours(1);

        // When
        boolean occupied = orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                3, OrderStatus.CANCELLED, checkFrom, checkTo);

        // Then
        assertFalse(occupied);
    }

    // -------------------------------------------------------------------------
    // findOrdersByItemStatuses (widok kuchni / kelnera)
    // -------------------------------------------------------------------------

    @Test
    void findOrdersByItemStatuses_shouldReturnOrdersWithMatchingItemStatus() {
        // Given - zamówienie OPEN z pozycją w statusie NEW
        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        // status domyślny NEW
        order.getOrderItems().add(item);
        orderRepository.save(order);

        // When - kuchnia szuka zamówień do przygotowania (NEW, PREPARING)
        List<Order> kitchenOrders = orderRepository.findOrdersByItemStatuses(
                List.of(OrderItemStatus.NEW, OrderItemStatus.PREPARING));

        // Then
        assertEquals(1, kitchenOrders.size());
    }

    @Test
    void findOrdersByItemStatuses_shouldNotReturnPaidOrders() {
        // Given - zamówienie PAID
        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        item.setStatus(OrderItemStatus.DELIVERED);
        order.getOrderItems().add(item);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // When
        List<Order> kitchenOrders = orderRepository.findOrdersByItemStatuses(
                List.of(OrderItemStatus.NEW, OrderItemStatus.PREPARING));

        // Then - PAID zamówienie nie trafia do kuchni
        assertTrue(kitchenOrders.isEmpty());
    }

    // -------------------------------------------------------------------------
    // helper
    // -------------------------------------------------------------------------

    private Order createPaidOrderWithItem(Dish dish, int quantity) {
        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, quantity, 1, null, order);
        item.setStatus(OrderItemStatus.DELIVERED);
        order.getOrderItems().add(item);
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }
}