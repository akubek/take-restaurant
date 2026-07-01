package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateOrderDTO;
import pl.polsl.take.restaurant.dto.CreateOrderItemDTO;
import pl.polsl.take.restaurant.dto.OrderDTO;
import pl.polsl.take.restaurant.dto.OrderItemDTO;
import pl.polsl.take.restaurant.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Operations for managing restaurant orders")
public class OrderController {

    private final OrderService service;

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Returns one order by identifier")
    public OrderDTO getOrder(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Get order items", description = "Returns all items in the selected order")
    public List<OrderItemDTO> getOrderItems(@PathVariable Long id) {
        return service.getOrderItems(id);
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Returns all orders")
    public CollectionModel<OrderDTO> getAll() {
        List<OrderDTO> orders = service.getAll();
        return CollectionModel.of(orders, linkTo(methodOn(OrderController.class).getAll()).withSelfRel());
    }

    @GetMapping("/kitchen")
    @Operation(summary = "Get kitchen queue", description = "Returns open orders containing NEW or PREPARING items")
    public CollectionModel<OrderDTO> getKitchenOrders() {
        List<OrderDTO> orders = service.getKitchenOrders();
        return CollectionModel.of(orders, linkTo(methodOn(OrderController.class).getKitchenOrders()).withSelfRel());
    }

    @GetMapping("/waiter")
    @Operation(summary = "Get waiter pickup queue", description = "Returns open orders containing READY items")
    public CollectionModel<OrderDTO> getWaiterPickupOrders() {
        List<OrderDTO> orders = service.getWaiterPickupOrders();
        return CollectionModel.of(orders,
                linkTo(methodOn(OrderController.class).getWaiterPickupOrders()).withSelfRel());
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Creates a new immediate order or future reservation")
    public OrderDTO create(@Valid @RequestBody CreateOrderDTO dto) {
        return service.create(dto);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add order item", description = "Adds a new item to an existing open order")
    public OrderDTO addItem(@PathVariable Long id, @Valid @RequestBody CreateOrderItemDTO dto) {
        return service.addItemToOrder(id, dto);
    }

    @PatchMapping("/{orderId}/items/{itemId}/status")
    @Operation(summary = "Update order item status", description = "Changes status of one item in the selected order")
    public OrderItemDTO updateItemStatus(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody pl.polsl.take.restaurant.dto.UpdateOrderItemStatusDTO dto) {

        service.updateItemStatus(orderId, itemId, dto.getStatus());
        return service.getOrderItem(orderId, itemId);
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @Operation(summary = "Cancel order item", description = "Marks one order item as cancelled")
    public void cancelItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        service.cancelOrderItem(orderId, itemId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancels the whole order and all its items")
    public void cancelOrder(@PathVariable Long id) {
        service.cancelOrder(id);
    }

    @PatchMapping("/{id}/pay")
    @Operation(summary = "Pay order", description = "Closes and marks the order as paid")
    public OrderDTO payOrder(@PathVariable Long id) {
        return service.payOrder(id);
    }
}