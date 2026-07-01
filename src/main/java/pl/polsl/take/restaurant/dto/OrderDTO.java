package pl.polsl.take.restaurant.dto;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import pl.polsl.take.restaurant.controller.CustomerController;
import pl.polsl.take.restaurant.controller.OrderController;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.enums.OrderStatus;

@Getter
@Schema(description = "Order response model")
public class OrderDTO extends RepresentationModel<OrderDTO> {

        private Long id;
        private LocalDateTime orderDateTime;
        private Integer tableNumber;
        @Schema(example = "OPEN")
        private OrderStatus status;

        private CustomerDTO customer;
        @JsonProperty("items")
        private List<OrderItemDTO> items;

        @Schema(description = "Total for non-cancelled items only, in cents/grosz", example = "6400")
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
                                .filter(item -> !item.getIsCancelled())
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