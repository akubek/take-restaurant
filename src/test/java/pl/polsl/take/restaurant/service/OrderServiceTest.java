package pl.polsl.take.restaurant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pl.polsl.take.restaurant.dto.CreateOrderDTO;
import pl.polsl.take.restaurant.dto.CreateOrderItemDTO;
import pl.polsl.take.restaurant.dto.OrderDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;
import pl.polsl.take.restaurant.model.enums.OrderStatus;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.OrderItemRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for order service lifecycle rules: reservation, item updates, cancellation, and payment.
 */
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    void shouldThrowWhenPayingOrderWithStatusNew() {
        // Given: order is still NEW and cannot be paid
        Order order = new Order(null, 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then: payment is rejected and nothing is persisted
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPayingAlreadyPaidOrder() {
        // Given: order is already paid
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then: second payment attempt is rejected
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }

    @Test
    void shouldThrowWhenPayingCancelledOrder() {
        // Given: order is cancelled
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then: payment attempt is rejected
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }

    @Test
    void shouldThrowWhenPayingOrderWithAllItemsCancelled() {
        // Given: order contains only cancelled items
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        OrderItem cancelledItem = new OrderItem(dish, 1, 1, null, order);
        cancelledItem.setIsCancelled(true);
        order.getOrderItems().add(cancelledItem);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then: payment is rejected because no active items remain
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }

    @Test
    void shouldThrowWhenPayingOrderWithItemsNotDelivered() {
        // Given: order has at least one item not delivered yet
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        OrderItem pendingItem = new OrderItem(dish, 1, 1, null, order);

        order.getOrderItems().add(pendingItem);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then: payment is rejected until all items are delivered
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }

    @Test
    void shouldSuccessfullyPayOrderWhenAllItemsDelivered() {
        // Given: open order with delivered items only
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 2, 1, null, order);
        item.setStatus(OrderItemStatus.DELIVERED);
        order.getOrderItems().add(item);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When: payment is executed
        OrderDTO result = orderService.payOrder(1L);

        // Then: order is marked as PAID and total is calculated
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(6400, result.getTotalPriceCents());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldCancelOpenOrderAndAllItsItems() {
        // Given: open order with multiple active items
        Dish dish = new Dish("Pizza", "", 1000, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        OrderItem item1 = new OrderItem(dish, 1, 1, null, order);
        OrderItem item2 = new OrderItem(dish, 1, 2, null, order);
        order.getOrderItems().addAll(List.of(item1, item2));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When: whole order is cancelled
        orderService.cancelOrder(1L);

        // Then: order and all items become cancelled
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(item1.getIsCancelled());
        assertTrue(item2.getIsCancelled());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldThrowWhenCancellingAlreadyPaidOrder() {
        // Given: order already paid
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then: cancellation is forbidden
        assertThrows(ConflictException.class, () -> orderService.cancelOrder(1L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAddingItemToClosedOrder() {
        // Given: order is paid and no longer modifiable
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        CreateOrderItemDTO dto = new CreateOrderItemDTO();
        dto.setDishId(1L);
        dto.setQuantity(1);

        // When / Then: adding a new item is rejected
        assertThrows(ConflictException.class, () -> orderService.addItemToOrder(1L, dto));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAddingItemToCancelledOrder() {
        // Given: order is cancelled
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        CreateOrderItemDTO dto = new CreateOrderItemDTO();
        dto.setDishId(1L);
        dto.setQuantity(1);

        // When / Then: adding a new item is rejected
        assertThrows(ConflictException.class, () -> orderService.addItemToOrder(1L, dto));
    }

    @Test
    void shouldThrowWhenCancellingItemInPaidOrder() {
        // Given: item belongs to order that is already paid
        Dish dish = new Dish("Pizza", "", 1000, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setStatus(OrderStatus.PAID);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: item cancellation is forbidden
        assertThrows(ConflictException.class, () -> orderService.cancelOrderItem(1L, 10L));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCreatingReservationWithoutCustomer() {
        // Given: reservation request without customer ID
        CreateOrderItemDTO itemDto = new CreateOrderItemDTO();
        itemDto.setDishId(1L);
        itemDto.setQuantity(1);

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setCustomerId(null);
        dto.setTableNumber(1);
        dto.setOrderDateTime(LocalDateTime.now().plusDays(1));
        dto.setItems(List.of(itemDto));

        // When / Then: reservation is rejected
        assertThrows(ConflictException.class, () -> orderService.create(dto));
    }

    @Test
    void shouldThrowWhenTableAlreadyReserved() {
        // Given: reservation request for table occupied in requested time window
        Customer customer = new Customer("Jan", "Kowalski", "123", "jan@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);

        when(customerRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                any(), any(), any(), any())).thenReturn(true);

        CreateOrderItemDTO itemDto = new CreateOrderItemDTO();
        itemDto.setDishId(1L);
        itemDto.setQuantity(1);

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setCustomerId(1L);
        dto.setTableNumber(5);
        dto.setOrderDateTime(LocalDateTime.now().plusDays(1));
        dto.setItems(List.of(itemDto));

        // When / Then: reservation is rejected due to conflict
        assertThrows(ConflictException.class, () -> orderService.create(dto));
    }

    @Test
    void shouldCreateOrderAndCalculateTotalCorrectly() {
        // Given: valid customer, active dish, and order payload
        Customer customer = new Customer("Jan", "Kowalski", "123456789", "jan.kowalski@example.com");
        ReflectionTestUtils.setField(customer, "id", 1L);

        Dish dish = new Dish("Pizza Margherita", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(customerRepository.findByIdAndIsActiveTrue(anyLong())).thenReturn(Optional.of(customer));
        when(dishRepository.findByIdAndIsActiveTrue(anyLong())).thenReturn(Optional.of(dish));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 99L);
            return o;
        });

        CreateOrderItemDTO itemDto = new CreateOrderItemDTO();
        itemDto.setDishId(1L);
        itemDto.setQuantity(2);
        itemDto.setSeatNumber(1);

        CreateOrderDTO orderDto = new CreateOrderDTO();
        orderDto.setCustomerId(1L);
        orderDto.setTableNumber(5);
        orderDto.setItems(List.of(itemDto));

        // When: order is created
        OrderDTO result = orderService.create(orderDto);

        // Then: saved order state and computed total are correct
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();

        assertEquals(OrderStatus.OPEN, saved.getStatus());
        assertEquals(5, saved.getTableNumber());
        assertEquals(1, saved.getOrderItems().size());
        assertEquals(2, saved.getOrderItems().get(0).getQuantity());
        assertEquals(customer, saved.getCustomer());

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(6400, result.getTotalPriceCents());
    }

    @Test
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        // Given: repository has no order for requested ID
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then: lookup fails with not-found exception
        assertThrows(NotFoundException.class, () -> orderService.getById(99L));
    }

    @Test
    void shouldSuccessfullyAddItemToOpenOrder() {
        // Given: open order and active dish to append
        Dish dish = new Dish("Burger", "", 2500, 700, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(dishRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        CreateOrderItemDTO dto = new CreateOrderItemDTO();
        dto.setDishId(1L);
        dto.setQuantity(2);
        dto.setSeatNumber(3);

        // When: new item is added
        OrderDTO result = orderService.addItemToOrder(1L, dto);

        // Then: order contains appended item and repository save is called
        assertEquals(1, order.getOrderItems().size());
        OrderItem added = order.getOrderItems().get(0);
        assertEquals(dish, added.getDish());
        assertEquals(2, added.getQuantity());
        assertEquals(3, added.getSeatNumber());
        verify(orderItemRepository).save(any(OrderItem.class));
        assertNotNull(result);
    }

    @Test
    void shouldReturnAllOrderItemsForOrder() {
        // Given: order contains multiple items
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item1 = new OrderItem(dish, 1, 1, null, order);
        OrderItem item2 = new OrderItem(dish, 2, 2, "extra cheese", order);
        order.getOrderItems().addAll(List.of(item1, item2));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When: all order items are requested
        var result = orderService.getOrderItems(1L);

        // Then: every item from the order is returned
        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnSingleOrderItemSuccessfully() {
        // Given: target item exists and belongs to requested order
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, "bez soli", order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When: specific item is requested
        var result = orderService.getOrderItem(1L, 10L);

        // Then: response contains selected item details
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("bez soli", result.getNotes());
    }

    @Test
    void shouldThrowConflictWhenOrderItemBelongsToDifferentOrder() {
        // Given: item exists but belongs to another order
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);

        Order order1 = new Order(null, 1);
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order(null, 2);
        ReflectionTestUtils.setField(order2, "id", 2L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order2);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: request is rejected with conflict
        assertThrows(ConflictException.class, () -> orderService.getOrderItem(1L, 10L));
    }

    @Test
    void shouldUpdateItemStatusSuccessfully() {
        // Given: item belongs to open order and can change status
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When: status is updated
        orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING);

        // Then: new status is persisted
        assertEquals(OrderItemStatus.PREPARING, item.getStatus());
        verify(orderItemRepository).save(item);
    }

    @Test
    void shouldThrowWhenUpdatingItemStatusAndItemBelongsToDifferentOrder() {
        // Given: item belongs to a different order
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);

        Order order1 = new Order(null, 1);
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order(null, 2);
        ReflectionTestUtils.setField(order2, "id", 2L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order2);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: status update is rejected
        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingItemStatusInPaidOrder() {
        // Given: order already paid
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setStatus(OrderStatus.PAID);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: status update is forbidden
        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingItemStatusInCancelledOrder() {
        // Given: order is cancelled
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setStatus(OrderStatus.CANCELLED);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: status updates are forbidden for cancelled order
        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingStatusOfAlreadyCancelledItem() {
        // Given: targeted item is already cancelled
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);
        item.setIsCancelled(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: status update is rejected
        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldSuccessfullyCancelOrderItem() {
        // Given: item belongs to open order
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When: item cancellation is requested
        orderService.cancelOrderItem(1L, 10L);

        // Then: item is marked as cancelled and persisted
        assertTrue(item.getIsCancelled());
        verify(orderItemRepository).save(item);
    }

    @Test
    void shouldThrowWhenCancellingItemFromDifferentOrder() {
        // Given: item belongs to different order than requested
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);

        Order order1 = new Order(null, 1);
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order(null, 2);
        ReflectionTestUtils.setField(order2, "id", 2L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order2);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then: cancellation is rejected with conflict
        assertThrows(ConflictException.class, () -> orderService.cancelOrderItem(1L, 10L));
        verify(orderItemRepository, never()).save(any());
    }

}