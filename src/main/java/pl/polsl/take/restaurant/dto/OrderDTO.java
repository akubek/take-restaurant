package pl.polsl.take.restaurant.dto;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.RepresentationModel;

import lombok.Getter;
import pl.polsl.take.restaurant.controller.CustomerController;
import pl.polsl.take.restaurant.controller.OrderController;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.enums.OrderStatus;

@Getter
public class OrderDTO extends RepresentationModel<OrderDTO> {

    private Long id;
    private LocalDateTime orderDateTime;
    private Integer tableNumber;
    private OrderStatus status;

    private CustomerDTO customer;
    private List<OrderItemDTO> items;

    private Integer totalPriceCents;
    
    public OrderDTO(Order order) {
        this.id = order.getId();
        this.orderDateTime = order.getOrderDateTime();
        this.status = order.getStatus();
        this.tableNumber = order.getTableNumber();

        if (order.getCustomer() != null) {
            this.customer = new CustomerDTO(order.getCustomer());
            
            add(linkTo(methodOn(CustomerController.class)
                .get(order.getCustomer().getId()))
                .withRel("customer"));
        }

        this.items = order.getOrderItems().stream()
                .map(OrderItemDTO::new)
                .collect(Collectors.toList());

        this.totalPriceCents = order.getOrderItems().stream()
                .filter(item -> !item.getIsCancelled()) // only count non-cancelled items toward the total price
                .mapToInt(item -> item.getDishPriceAtOrderTime() * item.getQuantity())
                .sum();

        add(linkTo(methodOn(OrderController.class)
                .getOrder(order.getId()))
                .withSelfRel());

        add(linkTo(methodOn(OrderController.class)
                .getOrderItems(order.getId()))
                .withRel("items"));
    }
}