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

// Mockito class to enable Mockito annotations like @Mock and @InjectMocks
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // create mock of OrderRepository
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    // inject order service with the mocked repository
    @InjectMocks
    private OrderService orderService;

    // capture objects passed to the repository's save method
    @Captor
    private ArgumentCaptor<Order> orderCaptor;


    // -------------------------------------------------------------------------
    // payOrder
    // -------------------------------------------------------------------------
 
    @Test
    void shouldThrowWhenPayingOrderWithStatusNew() {
        // Given - zamówienie OPEN (domyślny status)
        Order order = new Order(null, 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        // When / Then
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
        verify(orderRepository, never()).save(any());
    }
 
    @Test
    void shouldThrowWhenPayingAlreadyPaidOrder() {
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }
 
    @Test
    void shouldThrowWhenPayingCancelledOrder() {
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }
 
    @Test
    void shouldThrowWhenPayingOrderWithAllItemsCancelled() {
        // Given - zamówienie z jedną pozycją, ale pozycja anulowana → brak ważnych items
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        OrderItem cancelledItem = new OrderItem(dish, 1, 1, null, order);
        cancelledItem.setIsCancelled(true);
        order.getOrderItems().add(cancelledItem);
 
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        // When / Then - brak ważnych pozycji → nie można zapłacić
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }
 
    @Test
    void shouldThrowWhenPayingOrderWithItemsNotDelivered() {
        // Given - pozycja ma status NEW (nie DELIVERED) → kuchnia jej nie dostarczyła
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        OrderItem pendingItem = new OrderItem(dish, 1, 1, null, order);
        // status domyślny to NEW
        order.getOrderItems().add(pendingItem);
 
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        assertThrows(ConflictException.class, () -> orderService.payOrder(1L));
    }
 
    @Test
    void shouldSuccessfullyPayOrderWhenAllItemsDelivered() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
 
        OrderItem item = new OrderItem(dish, 2, 1, null, order);
        item.setStatus(OrderItemStatus.DELIVERED);
        order.getOrderItems().add(item);
 
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
 
        // When
        OrderDTO result = orderService.payOrder(1L);
 
        // Then
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(6400, result.getTotalPriceCents()); // 2 * 3200
        verify(orderRepository).save(order);
    }

    // -------------------------------------------------------------------------
    // cancelOrder
    // -------------------------------------------------------------------------
 
    @Test
    void shouldCancelOpenOrderAndAllItsItems() {
        // Given
        Dish dish = new Dish("Pizza", "", 1000, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        OrderItem item1 = new OrderItem(dish, 1, 1, null, order);
        OrderItem item2 = new OrderItem(dish, 1, 2, null, order);
        order.getOrderItems().addAll(List.of(item1, item2));
 
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
 
        // When
        orderService.cancelOrder(1L);
 
        // Then - zamówienie i wszystkie pozycje anulowane
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(item1.getIsCancelled());
        assertTrue(item2.getIsCancelled());
        verify(orderRepository).save(order);
    }
 
    @Test
    void shouldThrowWhenCancellingAlreadyPaidOrder() {
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        assertThrows(ConflictException.class, () -> orderService.cancelOrder(1L));
        verify(orderRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // addItemToOrder
    // -------------------------------------------------------------------------
 
    @Test
    void shouldThrowWhenAddingItemToClosedOrder() {
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        CreateOrderItemDTO dto = new CreateOrderItemDTO();
        dto.setDishId(1L);
        dto.setQuantity(1);
 
        assertThrows(ConflictException.class, () -> orderService.addItemToOrder(1L, dto));
        verify(orderItemRepository, never()).save(any());
    }
 
    @Test
    void shouldThrowWhenAddingItemToCancelledOrder() {
        Order order = new Order(null, 1);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
 
        CreateOrderItemDTO dto = new CreateOrderItemDTO();
        dto.setDishId(1L);
        dto.setQuantity(1);
 
        assertThrows(ConflictException.class, () -> orderService.addItemToOrder(1L, dto));
    }
 
    // -------------------------------------------------------------------------
    // cancelOrderItem
    // -------------------------------------------------------------------------
 
    @Test
    void shouldThrowWhenCancellingItemInPaidOrder() {
        Dish dish = new Dish("Pizza", "", 1000, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setStatus(OrderStatus.PAID);
 
        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);
 
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));
 
        assertThrows(ConflictException.class, () -> orderService.cancelOrderItem(1L, 10L));
        verify(orderItemRepository, never()).save(any());
    }
 
    // -------------------------------------------------------------------------
    // create - rezerwacje
    // -------------------------------------------------------------------------
 
    @Test
    void shouldThrowWhenCreatingReservationWithoutCustomer() {
        // Given - rezerwacja na przyszłość bez klienta (anonimowe rezerwacje są zabronione)
        CreateOrderItemDTO itemDto = new CreateOrderItemDTO();
        itemDto.setDishId(1L);
        itemDto.setQuantity(1);
 
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setCustomerId(null);
        dto.setTableNumber(1);
        dto.setOrderDateTime(LocalDateTime.now().plusDays(1));
        dto.setItems(List.of(itemDto));
 
        // When / Then
        assertThrows(ConflictException.class, () -> orderService.create(dto));
    }
 
    @Test
    void shouldThrowWhenTableAlreadyReserved() {
        // Given - stolik zajęty w danym przedziale czasowym
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
 
        // When / Then
        assertThrows(ConflictException.class, () -> orderService.create(dto));
    }
 
    // -------------------------------------------------------------------------
    // create - happy path (rozszerzenie istniejącego testu)
    // -------------------------------------------------------------------------
 
    @Test
    void shouldCreateOrderAndCalculateTotalCorrectly() {
        // Given
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
 
        // When
        OrderDTO result = orderService.create(orderDto);
 
        // Then
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
 
        assertEquals(OrderStatus.OPEN, saved.getStatus());
        assertEquals(5, saved.getTableNumber());
        assertEquals(1, saved.getOrderItems().size());
        assertEquals(2, saved.getOrderItems().get(0).getQuantity());
        assertEquals(customer, saved.getCustomer());
 
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(6400, result.getTotalPriceCents()); // 2 * 3200
    }
 
    @Test
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
 
        assertThrows(NotFoundException.class, () -> orderService.getById(99L));
    }

    // -------------------------------------------------------------------------
    // addItemToOrder - happy path
    // -------------------------------------------------------------------------

    @Test
    void shouldSuccessfullyAddItemToOpenOrder() {
        // Given
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

        // When
        OrderDTO result = orderService.addItemToOrder(1L, dto);

        // Then
        assertEquals(1, order.getOrderItems().size());
        OrderItem added = order.getOrderItems().get(0);
        assertEquals(dish, added.getDish());
        assertEquals(2, added.getQuantity());
        assertEquals(3, added.getSeatNumber());
        verify(orderItemRepository).save(any(OrderItem.class));
        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // getOrderItems / getOrderItem
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnAllOrderItemsForOrder() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item1 = new OrderItem(dish, 1, 1, null, order);
        OrderItem item2 = new OrderItem(dish, 2, 2, "extra cheese", order);
        order.getOrderItems().addAll(List.of(item1, item2));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        var result = orderService.getOrderItems(1L);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnSingleOrderItemSuccessfully() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, "bez soli", order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When
        var result = orderService.getOrderItem(1L, 10L);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("bez soli", result.getNotes());
    }

    @Test
    void shouldThrowConflictWhenOrderItemBelongsToDifferentOrder() {
        // Given - pozycja należy do zamówienia 2, ale pytamy w kontekście zamówienia 1
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);

        Order order1 = new Order(null, 1);
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order(null, 2);
        ReflectionTestUtils.setField(order2, "id", 2L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order2); // należy do zamówienia 2
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then
        assertThrows(ConflictException.class, () -> orderService.getOrderItem(1L, 10L));
    }

    // -------------------------------------------------------------------------
    // updateItemStatus
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateItemStatusSuccessfully() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);
        // status domyślny: NEW

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When
        orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING);

        // Then
        assertEquals(OrderItemStatus.PREPARING, item.getStatus());
        verify(orderItemRepository).save(item);
    }

    @Test
    void shouldThrowWhenUpdatingItemStatusAndItemBelongsToDifferentOrder() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);

        Order order1 = new Order(null, 1);
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order(null, 2);
        ReflectionTestUtils.setField(order2, "id", 2L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order2); // należy do zamówienia 2
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingItemStatusInPaidOrder() {
        // Given - zamówienie opłacone → statusów pozycji nie można zmieniać
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setStatus(OrderStatus.PAID);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingItemStatusInCancelledOrder() {
        // Given - zamówienie anulowane → statusów pozycji nie można zmieniać
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.setStatus(OrderStatus.CANCELLED);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingStatusOfAlreadyCancelledItem() {
        // Given - sama pozycja jest anulowana (np. klient zmienił zdanie)
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1); // zamówienie OPEN
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);
        item.setIsCancelled(true); // pozycja anulowana

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ConflictException.class, () -> orderService.updateItemStatus(1L, 10L, OrderItemStatus.PREPARING));
        verify(orderItemRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // cancelOrderItem - happy path & mismatch
    // -------------------------------------------------------------------------

    @Test
    void shouldSuccessfullyCancelOrderItem() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);
        Order order = new Order(null, 1);
        ReflectionTestUtils.setField(order, "id", 1L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When
        orderService.cancelOrderItem(1L, 10L);

        // Then
        assertTrue(item.getIsCancelled());
        verify(orderItemRepository).save(item);
    }

    @Test
    void shouldThrowWhenCancellingItemFromDifferentOrder() {
        // Given
        Dish dish = new Dish("Pizza", "", 3200, 500, SpicinessLevel.MILD);

        Order order1 = new Order(null, 1);
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order(null, 2);
        ReflectionTestUtils.setField(order2, "id", 2L);

        OrderItem item = new OrderItem(dish, 1, 1, null, order2); // należy do zamówienia 2
        ReflectionTestUtils.setField(item, "id", 10L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When / Then
        assertThrows(ConflictException.class, () -> orderService.cancelOrderItem(1L, 10L));
        verify(orderItemRepository, never()).save(any());
    }

}