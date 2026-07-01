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

@DataJpaTest
/**
 * Repository tests for order-level aggregate queries and table reservation filters.
 */
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DishRepository dishRepository;

    private Customer customer;
    private Dish dish;

    @BeforeEach
    void setUp() {

        orderRepository.deleteAll();
        dishRepository.deleteAll();
        customerRepository.deleteAll();

        customer = customerRepository.save(
                new Customer("Jan", "Kowalski", "123456789", "jan@test.com"));
        dish = dishRepository.save(
                new Dish("Pizza Margherita", "Klasyczna pizza", 3200, 800, SpicinessLevel.MILD));
    }

    @Test
    void sumRevenueBetween_shouldReturnCorrectSumForPaidOrders() {

        createPaidOrderWithItem(dish, 2);

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        assertTrue(revenue.isPresent());
        assertEquals(6400L, revenue.get());
    }

    @Test
    void sumRevenueBetween_shouldReturnEmptyWhenNoOrdersInRange() {

        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now().minusDays(1);

        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        assertTrue(revenue.isEmpty());
    }

    @Test
    void sumRevenueBetween_shouldIgnoreCancelledOrders() {

        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 3, 1, null, order);
        order.getOrderItems().add(item);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        assertTrue(revenue.isEmpty());
    }

    @Test
    void sumRevenueBetween_shouldIgnoreCancelledItemsInPaidOrder() {

        Order order = new Order(customer, 1);

        OrderItem validItem = new OrderItem(dish, 1, 1, null, order);
        OrderItem cancelledItem = new OrderItem(dish, 5, 2, null, order);
        cancelledItem.setIsCancelled(true);

        order.getOrderItems().addAll(List.of(validItem, cancelledItem));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        Optional<Long> revenue = orderRepository.sumRevenueBetween(from, to);

        assertTrue(revenue.isPresent());
        assertEquals(3200L, revenue.get());
    }

    @Test
    void sumCustomerSpending_shouldReturnTotalForCustomer() {

        createPaidOrderWithItem(dish, 1);
        createPaidOrderWithItem(dish, 2);

        Optional<Long> spending = orderRepository.sumCustomerSpending(customer.getId());

        assertTrue(spending.isPresent());
        assertEquals(9600L, spending.get());
    }

    @Test
    void sumCustomerSpending_shouldReturnEmptyForCustomerWithNoOrders() {

        Customer newCustomer = customerRepository.save(
                new Customer("Anna", "Nowak", null, null));

        Optional<Long> spending = orderRepository.sumCustomerSpending(newCustomer.getId());

        assertTrue(spending.isEmpty());
    }

    @Test
    void sumCustomerSpending_shouldNotCountOpenOrders() {

        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        order.getOrderItems().add(item);

        orderRepository.save(order);

        Optional<Long> spending = orderRepository.sumCustomerSpending(customer.getId());

        assertTrue(spending.isEmpty());
    }

    @Test
    void tableAvailability_shouldReturnTrueWhenTableOccupied() {

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Order reservation = new Order(customer, 5, tomorrow);

        orderRepository.save(reservation);

        LocalDateTime checkFrom = tomorrow.minusHours(1);
        LocalDateTime checkTo = tomorrow.plusHours(1);

        boolean occupied = orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                5, OrderStatus.CANCELLED, checkFrom, checkTo);

        assertTrue(occupied);
    }

    @Test
    void tableAvailability_shouldReturnFalseForCancelledReservation() {

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Order reservation = new Order(customer, 5, tomorrow);
        reservation.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(reservation);

        LocalDateTime checkFrom = tomorrow.minusHours(1);
        LocalDateTime checkTo = tomorrow.plusHours(1);

        boolean occupied = orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                5, OrderStatus.CANCELLED, checkFrom, checkTo);

        assertFalse(occupied);
    }

    @Test
    void tableAvailability_shouldReturnFalseForDifferentTable() {

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Order reservation = new Order(customer, 5, tomorrow);
        orderRepository.save(reservation);

        LocalDateTime checkFrom = tomorrow.minusHours(1);
        LocalDateTime checkTo = tomorrow.plusHours(1);

        boolean occupied = orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                3, OrderStatus.CANCELLED, checkFrom, checkTo);

        assertFalse(occupied);
    }

    @Test
    void findOrdersByItemStatuses_shouldReturnOrdersWithMatchingItemStatus() {

        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 1, 1, null, order);

        order.getOrderItems().add(item);
        orderRepository.save(order);

        List<Order> kitchenOrders = orderRepository.findOrdersByItemStatuses(
                List.of(OrderItemStatus.NEW, OrderItemStatus.PREPARING));

        assertEquals(1, kitchenOrders.size());
    }

    @Test
    void findOrdersByItemStatuses_shouldNotReturnPaidOrders() {

        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        item.setStatus(OrderItemStatus.DELIVERED);
        order.getOrderItems().add(item);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        List<Order> kitchenOrders = orderRepository.findOrdersByItemStatuses(
                List.of(OrderItemStatus.NEW, OrderItemStatus.PREPARING));

        assertTrue(kitchenOrders.isEmpty());
    }

    private Order createPaidOrderWithItem(Dish dish, int quantity) {
        Order order = new Order(customer, 1);
        OrderItem item = new OrderItem(dish, quantity, 1, null, order);
        item.setStatus(OrderItemStatus.DELIVERED);
        order.getOrderItems().add(item);
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }
}