package pl.polsl.take.restaurant.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateOrderDTO;
import pl.polsl.take.restaurant.dto.CreateOrderItemDTO;
import pl.polsl.take.restaurant.dto.OrderDTO;
import pl.polsl.take.restaurant.dto.OrderItemDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;
import pl.polsl.take.restaurant.model.enums.OrderStatus;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.OrderItemRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final DishRepository dishRepo;
    private final OrderItemRepository orderItemRepo;

    // Założony, bezpieczny czas trwania wizyty dla rezerwacji
    private static final long RESERVATION_MARGIN_HOURS = 2;

    @Transactional(readOnly = true)
    public OrderDTO getById(Long id) {
        return new OrderDTO(findOrderById(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getByCustomerId(Long customerId) {
        return orderRepo.findByCustomerId(customerId).stream()
                .map(OrderDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderItemDTO> getOrderItems(Long orderId) {
        Order order = findOrderById(orderId);
        return order.getOrderItems().stream()
                .map(OrderItemDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAll() {
        return orderRepo.findAll().stream()
                .map(OrderDTO::new)
                .toList();
    }

    @Transactional
    public OrderDTO create(CreateOrderDTO dto) {
        Customer customer = null;
        if (dto.getCustomerId() != null) {
            customer = customerRepo.findByIdAndIsActiveTrue(dto.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Klient nie istnieje."));
        }

        Order order;

        // reservation
        if (dto.getOrderDateTime() != null) {
            if (customer == null) {
                throw new ConflictException("Rezerwacja na przyszłość wymaga podania przypisanego klienta.");
            }
            
            // check if the reservation date is in the future
            if (dto.getOrderDateTime().isBefore(LocalDateTime.now())) {
                throw new ConflictException("Data rezerwacji musi być w przyszłości.");
            }

            // check table availability for the requested time slot
            checkTableAvailability(dto.getTableNumber(), dto.getOrderDateTime(), RESERVATION_MARGIN_HOURS);
            
            // create future order
            order = new Order(customer, dto.getTableNumber(), dto.getOrderDateTime());
            
        } 
        // order for now
        else {
            // default constructor - current time
            order = new Order(customer, dto.getTableNumber());
        }

        // map order items from DTO to OrderItem entities and add them to the order
        for (CreateOrderItemDTO itemDto : dto.getItems()) {
            Dish dish = findActiveDish(itemDto.getDishId());
            order.getOrderItems().add(
                    new OrderItem(dish, itemDto.getQuantity(), itemDto.getSeatNumber(), itemDto.getNotes(), order)
            );
        }

        return new OrderDTO(orderRepo.save(order));
    }

    @Transactional
    public OrderDTO addItemToOrder(Long orderId, CreateOrderItemDTO itemDto) {
        Order order = findOrderById(orderId);

        // block adding order items to closed (cancelled or paid) orders
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new ConflictException("Can only add items to open orders.");
        }

        Dish dish = findActiveDish(itemDto.getDishId());
        OrderItem newItem = new OrderItem(dish, itemDto.getQuantity(), itemDto.getSeatNumber(), itemDto.getNotes(), order);
        
        order.getOrderItems().add(newItem);
        orderItemRepo.save(newItem);

        return new OrderDTO(order);
    }

    @Transactional
    public void updateItemStatus(Long orderId, Long itemId, OrderItemStatus newStatus) {
        Order order = findOrderById(orderId);
        OrderItem item = findOrderItemById(itemId);

        if (!item.getOrder().getId().equals(order.getId())) {
            throw new ConflictException("This item does not belong to the specified order.");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Cannot change the status of items in a paid order.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Cannot change the status of items in a cancelled order.");
        }
        if (item.getIsCancelled()) {
            throw new ConflictException("Cannot change the status of a cancelled item.");
        }

        item.setStatus(newStatus);
        orderItemRepo.save(item);
    }

    @Transactional
    public void cancelOrderItem(Long orderId, Long itemId) {
        Order order = findOrderById(orderId);
        OrderItem item = findOrderItemById(itemId);

        // Ensure the item belongs to the given order
        if (!item.getOrder().getId().equals(order.getId())) {
            throw new ConflictException("This item does not belong to the specified order.");
        }

        // Rule: Editing/deleting is only possible when the order is not yet paid
        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Cannot modify items in an order that has already been paid.");
        }

        item.setIsCancelled(true);
        orderItemRepo.save(item);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Cannot cancel an order that has already been paid.");
        }

        // soft delete the order
        order.setStatus(OrderStatus.CANCELLED);
        
        // cancel all order items associated with this order
        for (OrderItem item : order.getOrderItems()) item.setIsCancelled(true);

        orderRepo.save(order);
    }

    @Transactional
    public OrderDTO payOrder(Long orderId) {
        Order order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("The order was already paid.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Cannot pay a cancelled order.");
        }

        // check if the order has any valid (non-cancelled) items
        boolean hasValidItems = order.getOrderItems().stream()
        .anyMatch(item -> !item.getIsCancelled());

        if (!hasValidItems) {
            throw new ConflictException("Cannot pay an empty order. Please cancel the order instead.");
        }

        // CHECK: Are there any unfinished dishes?
        boolean hasPendingItems = order.getOrderItems().stream()
                .filter(item -> !item.getIsCancelled()) // Ignore cancelled items
                .anyMatch(item -> item.getStatus() != OrderItemStatus.DELIVERED);

        if (hasPendingItems) {
            throw new ConflictException("Cannot close the bill. The order contains items that have not yet been delivered to the customer or cancelled.");
        }

        order.setStatus(OrderStatus.PAID);
        return new OrderDTO(orderRepo.save(order));
    }

    // kitchen view - new and preparing orders
    @Transactional(readOnly = true)
    public List<OrderDTO> getKitchenOrders() {
        return orderRepo.findOrdersByItemStatuses(List.of(OrderItemStatus.NEW, OrderItemStatus.PREPARING))
                .stream().map(OrderDTO::new).toList();
    }

    // waiter view - ready for pickup orders
    @Transactional(readOnly = true)
    public List<OrderDTO> getWaiterPickupOrders() {
        return orderRepo.findOrdersByItemStatuses(List.of(OrderItemStatus.READY))
                .stream().map(OrderDTO::new).toList();
    }

    private Order findOrderById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Zamówienie o ID " + id + " nie istnieje."));
    }

    private OrderItem findOrderItemById(Long id) {
        return orderItemRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Pozycja zamówienia o ID " + id + " nie istnieje."));
    }

    private Dish findActiveDish(Long id) {
        return dishRepo.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Danie nie istnieje lub zostało wycofane z menu."));
    }

    private void checkTableAvailability(Integer tableNumber, LocalDateTime requestedTime, Long reservationMarginHours) {
        LocalDateTime startTime = requestedTime.minusHours(reservationMarginHours);
        LocalDateTime endTime = requestedTime.plusHours(reservationMarginHours);

        boolean isOccupied = orderRepo.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                tableNumber, OrderStatus.CANCELLED, startTime, endTime
        );

        if (isOccupied) {
            throw new ConflictException("Table number " + tableNumber + " is already reserved in the time slot from " + startTime + " to " + endTime + ".");
        }
    }
}