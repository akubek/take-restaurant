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

/**
 * Handles order lifecycle, reservation validation, item state changes, and payment checks.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final DishRepository dishRepo;
    private final OrderItemRepository orderItemRepo;

    /**
     * Time window used to detect table reservation overlaps before and after requested time.
     */
    private static final long RESERVATION_MARGIN_HOURS = 2;

    /**
     * Returns order by ID.
     *
     * @param id order ID
     * @return order DTO
     */
    @Transactional(readOnly = true)
    public OrderDTO getById(Long id) {
        return new OrderDTO(findOrderById(id));
    }

    /**
     * Returns all orders for a customer.
     *
     * @param customerId customer ID
     * @return list of order DTOs
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getByCustomerId(Long customerId) {
        return orderRepo.findByCustomerId(customerId).stream()
                .map(OrderDTO::new)
                .toList();
    }

    /**
     * Returns all items in selected order.
     *
     * @param orderId order ID
     * @return list of item DTOs
     */
    @Transactional(readOnly = true)
    public List<OrderItemDTO> getOrderItems(Long orderId) {
        Order order = findOrderById(orderId);
        return order.getOrderItems().stream()
                .map(OrderItemDTO::new)
                .toList();
    }

    /**
     * Returns one order item and verifies that it belongs to provided order.
     *
     * @param orderId order ID
     * @param itemId order item ID
     * @return item DTO
     */
    @Transactional(readOnly = true)
    public OrderItemDTO getOrderItem(Long orderId, Long itemId) {
        Order order = findOrderById(orderId);
        OrderItem item = findOrderItemById(itemId);

        if (!item.getOrder().getId().equals(order.getId())) {
            throw new ConflictException("This item does not belong to the specified order.");
        }

        return new OrderItemDTO(item);
    }

    /**
     * Returns all orders.
     *
     * @return list of order DTOs
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getAll() {
        return orderRepo.findAll().stream()
                .map(OrderDTO::new)
                .toList();
    }

    /**
     * Creates an immediate order or a future reservation depending on orderDateTime.
     *
     * @param dto order creation payload
     * @return created order DTO
     */
    @Transactional
    public OrderDTO create(CreateOrderDTO dto) {
        Customer customer = null;
        if (dto.getCustomerId() != null) {
            customer = customerRepo.findByIdAndIsActiveTrue(dto.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Client not found."));
        }

        Order order;

        if (dto.getOrderDateTime() != null) {
            if (customer == null) {
                throw new ConflictException("Future reservations require an assigned customer.");
            }

            if (dto.getOrderDateTime().isBefore(LocalDateTime.now())) {
                throw new ConflictException("Reservation date must be in the future.");
            }

            checkTableAvailability(dto.getTableNumber(), dto.getOrderDateTime(), RESERVATION_MARGIN_HOURS);

            order = new Order(customer, dto.getTableNumber(), dto.getOrderDateTime());

        }

        else {

            order = new Order(customer, dto.getTableNumber());
        }

        for (CreateOrderItemDTO itemDto : dto.getItems()) {
            Dish dish = findActiveDish(itemDto.getDishId());
            order.getOrderItems().add(
                    new OrderItem(dish, itemDto.getQuantity(), itemDto.getSeatNumber(), itemDto.getNotes(), order));
        }

        return new OrderDTO(orderRepo.save(order));
    }

    /**
     * Adds item to an open order.
     *
     * @param orderId order ID
     * @param itemDto item payload
     * @return updated order DTO
     */
    @Transactional
    public OrderDTO addItemToOrder(Long orderId, CreateOrderItemDTO itemDto) {
        Order order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new ConflictException("Can only add items to open orders.");
        }

        Dish dish = findActiveDish(itemDto.getDishId());
        OrderItem newItem = new OrderItem(dish, itemDto.getQuantity(), itemDto.getSeatNumber(), itemDto.getNotes(),
                order);

        order.getOrderItems().add(newItem);
        orderItemRepo.save(newItem);

        return new OrderDTO(order);
    }

    /**
     * Updates status of one order item.
     *
     * @param orderId order ID
     * @param itemId item ID
     * @param newStatus target status
     */
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

    /**
     * Soft-cancels one order item.
     *
     * @param orderId order ID
     * @param itemId item ID
     */
    @Transactional
    public void cancelOrderItem(Long orderId, Long itemId) {
        Order order = findOrderById(orderId);
        OrderItem item = findOrderItemById(itemId);

        if (!item.getOrder().getId().equals(order.getId())) {
            throw new ConflictException("This item does not belong to the specified order.");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Cannot modify items in an order that has already been paid.");
        }

        item.setIsCancelled(true);
        orderItemRepo.save(item);
    }

    /**
     * Marks a full order as cancelled and cancels all of its items.
     *
     * @param orderId order ID
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Cannot cancel an order that has already been paid.");
        }

        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getOrderItems())
            item.setIsCancelled(true);

        orderRepo.save(order);
    }

    /**
     * Marks an order as paid only when all non-cancelled items are delivered.
     *
     * @param orderId order ID
     * @return paid order DTO
     */
    @Transactional
    public OrderDTO payOrder(Long orderId) {
        Order order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("The order was already paid.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Cannot pay a cancelled order.");
        }

        boolean hasValidItems = order.getOrderItems().stream()
                .anyMatch(item -> !item.getIsCancelled());

        if (!hasValidItems) {
            throw new ConflictException("Cannot pay an empty order. Please cancel the order instead.");
        }

        boolean hasPendingItems = order.getOrderItems().stream()
                .filter(item -> !item.getIsCancelled())
                .anyMatch(item -> item.getStatus() != OrderItemStatus.DELIVERED);

        if (hasPendingItems) {
            throw new ConflictException(
                    "Cannot close the bill. The order contains items that have not yet been delivered to the customer or cancelled.");
        }

        order.setStatus(OrderStatus.PAID);
        return new OrderDTO(orderRepo.save(order));
    }

    /**
     * Returns open orders that still have NEW or PREPARING items.
     *
     * @return list of kitchen-visible orders
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getKitchenOrders() {
        return orderRepo.findOrdersByItemStatuses(List.of(OrderItemStatus.NEW, OrderItemStatus.PREPARING))
                .stream().map(OrderDTO::new).toList();
    }

    /**
     * Returns open orders that have READY items waiting for pickup.
     *
     * @return list of waiter-visible orders
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getWaiterPickupOrders() {
        return orderRepo.findOrdersByItemStatuses(List.of(OrderItemStatus.READY))
                .stream().map(OrderDTO::new).toList();
    }

    private Order findOrderById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Order with ID " + id + " does not exist."));
    }

    private OrderItem findOrderItemById(Long id) {
        return orderItemRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Order item with ID " + id + " does not exist."));
    }

    private Dish findActiveDish(Long id) {
        return dishRepo.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException(
                        "Dish with ID " + id + " does not exist or has been removed from the menu."));
    }

    private void checkTableAvailability(Integer tableNumber, LocalDateTime requestedTime, Long reservationMarginHours) {
        LocalDateTime startTime = requestedTime.minusHours(reservationMarginHours);
        LocalDateTime endTime = requestedTime.plusHours(reservationMarginHours);

        boolean isOccupied = orderRepo.existsByTableNumberAndStatusNotAndOrderDateTimeBetween(
                tableNumber, OrderStatus.CANCELLED, startTime, endTime);

        if (isOccupied) {
            throw new ConflictException("Table number " + tableNumber + " is already reserved in the time slot from "
                    + startTime + " to " + endTime + ".");
        }
    }
}