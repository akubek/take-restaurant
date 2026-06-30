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
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;
import pl.polsl.take.restaurant.model.enums.OrderStatus;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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

    // inject order service with the mocked repository
    @InjectMocks
    private OrderService orderService;

    // capture objects passed to the repository's save method
    @Captor
    private ArgumentCaptor<Order> orderCaptor;


    //TEST 1: Paying for an order that is still OPEN should throw a ConflictException
    @Test
    void shouldThrowConflictExceptionWhenPayingForNewOrder() {
        // Given mock order with status OPEN
        Order mockOrder = mock(Order.class);
        //when(mockOrder.getId()).thenReturn(1L);
        when(mockOrder.getStatus()).thenReturn(OrderStatus.OPEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        // When pay order with id 1 & Then expect ConflictException to be thrown
        assertThrows(ConflictException.class, () -> {
            orderService.payOrder(1L);
        });
        
        // Optionally: Verify that the service did NOT attempt to save the order to the repository
        verify(orderRepository, never()).save(any(Order.class));
    }

    // TEST 2: Paying for an order that is OPEN and has all items DELIVERED should succeed and change the order status to PAID
    @Test
    void shouldSuccessfullyPayOrderWhenAllItemsDelivered() {
        // Given order with all items DELIVERED
        Dish realDish = new Dish( "Test Dish", "", 99, 100, SpicinessLevel.MILD);

        Order realOrder = new Order(null, 5);
        ReflectionTestUtils.setField(realOrder, "id", 1L);

        OrderItem realItem = new OrderItem(realDish, 1, 1, "Test notes", realOrder);
        realItem.setStatus(OrderItemStatus.DELIVERED);
        
        realOrder.getOrderItems().add(realItem);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(realOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // When order is paid for
        orderService.payOrder(1L);

        // Then the order status should be updated to PAID and saved
        assertEquals(OrderStatus.PAID, realOrder.getStatus());
        verify(orderRepository).save(realOrder);
    }

    @Test
    void shouldCreateOrderAndCalculateTotalCorrectly() {
        // Given
        // client with id 1
        Customer realCustomer = new Customer("Jan", "Kowalski", "123456789", "jan.kowalski@example.com");
        ReflectionTestUtils.setField(realCustomer, "id", 1L);

        // dish with price 32.00 ( 3200 in cents), id 1
        Dish realDish = new Dish("Pizza Margherita", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(realDish, "id", 1L);

        when(customerRepository.findByIdAndIsActiveTrue(anyLong()))
            .thenReturn(Optional.of(realCustomer));

        when(dishRepository.findByIdAndIsActiveTrue(anyLong()))
            .thenReturn(Optional.of(realDish));

        // 'save' method for order repository should return the order with an ID set (simulating database behavior)
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(orderToSave, "id", 99L);
            return orderToSave;
        });

        // Test user input DTO
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
        // check that the order was saved with the correct properties
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals(OrderStatus.OPEN, savedOrder.getStatus());
        assertEquals(5, savedOrder.getTableNumber());
        assertEquals(1, savedOrder.getOrderItems().size());
        assertEquals(2, savedOrder.getOrderItems().get(0).getQuantity());
        assertEquals(realCustomer, savedOrder.getCustomer()); // Ensure the customer is assigned

        // check output DTO and total price calculation
        assertNotNull(result);
        assertEquals(99L, result.getId(), "DTO should have the same ID as the saved order");
        assertEquals(6400, result.getTotalPriceCents(), "2 x 3200 cents should equal 6400");
    }
}