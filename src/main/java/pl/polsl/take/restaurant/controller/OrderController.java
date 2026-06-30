package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

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
public class OrderController {

    private final OrderService service;

    @GetMapping("/{id}")
    public OrderDTO getOrder(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/items")
    public List<OrderItemDTO> getOrderItems(@PathVariable Long id) {
        return service.getOrderItems(id);
    }
    
    @GetMapping
    public CollectionModel<OrderDTO> getAll() {
        List<OrderDTO> orders = service.getAll();
        return CollectionModel.of(orders, linkTo(methodOn(OrderController.class).getAll()).withSelfRel());
    }

    @GetMapping("/kitchen")
    public CollectionModel<OrderDTO> getKitchenOrders() {
        List<OrderDTO> orders = service.getKitchenOrders();
        return CollectionModel.of(orders, linkTo(methodOn(OrderController.class).getKitchenOrders()).withSelfRel());
    }

    @GetMapping("/waiter")
    public CollectionModel<OrderDTO> getWaiterPickupOrders() {
        List<OrderDTO> orders = service.getWaiterPickupOrders();
        return CollectionModel.of(orders, linkTo(methodOn(OrderController.class).getWaiterPickupOrders()).withSelfRel());
    }

    @PostMapping
    public OrderDTO create(@Valid @RequestBody CreateOrderDTO dto) {
        return service.create(dto);
    }

    @PostMapping("/{id}/items")
    public OrderDTO addItem(@PathVariable Long id, @Valid @RequestBody CreateOrderItemDTO dto) {
        return service.addItemToOrder(id, dto);
    }

    @PatchMapping("/{orderId}/items/{itemId}/status")
    public void updateItemStatus(
            @PathVariable Long orderId, 
            @PathVariable Long itemId, 
            @Valid @RequestBody pl.polsl.take.restaurant.dto.UpdateOrderItemStatusDTO dto) {
        
        service.updateItemStatus(orderId, itemId, dto.getStatus());
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public void cancelItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        service.cancelOrderItem(orderId, itemId);
    }

    @DeleteMapping("/{id}")
    public void cancelOrder(@PathVariable Long id) {
        service.cancelOrder(id);
    }

    @PatchMapping("/{id}/pay")
    public OrderDTO payOrder(@PathVariable Long id) {
        return service.payOrder(id);
    }
}