package pl.polsl.take.restaurant.model.DTOs;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.hateoas.RepresentationModel;

import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.controller.CustomerController;
import pl.polsl.take.restaurant.controller.OrderController;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.model.enums.OrderStatus;

@Getter
public class OrderDTO extends RepresentationModel<OrderDTO> {

    private Long id;
    private Customer customer;
    private LocalDateTime orderDateTime;
    private Integer tableNumber;
    private OrderStatus status;
    
    public OrderDTO(Order order) {
        this.id = order.getId();
        this.orderDateTime = order.getOrderDateTime();
        this.status = order.getStatus();
        this.tableNumber = order.getTableNumber();

        add(linkTo(methodOn(OrderController.class)
                .getOrder(order.getId()))
                .withSelfRel());

        add(linkTo(methodOn(OrderController.class)
                .getItems(order.getId()))
                .withRel("items"));

        add(linkTo(methodOn(CustomerController.class)
                .get(order.getCustomer().getId()))
                .withRel("customer"));
    }
}