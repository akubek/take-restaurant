package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.OrderService;
import pl.polsl.take.restaurant.model.DTOs.CreateOrderDTO;
import pl.polsl.take.restaurant.model.DTOs.OrderDTO;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public OrderDTO create(@RequestBody CreateOrderDTO dto) {
        return new OrderDTO(service.create(dto));
    }

    @GetMapping("/{id}")
    public OrderDTO getOrder(@PathVariable Long id) {
        return new OrderDTO(service.getById(id));
    }
    
    @GetMapping("/{id}/items")
    public List<String> getItems(@PathVariable Long id) {
        return service.getById(id).getOrderItems().stream().map(ri -> ri.getDish().getName()).toList();
    }
    
    @GetMapping
    public CollectionModel<OrderDTO> getAll() {

        List<OrderDTO> orders = service.getAll()
                .stream()
                .map(OrderDTO::new)
                .toList();

        return CollectionModel.of(
                orders,
                linkTo(methodOn(OrderController.class)
                        .getAll())
                        .withSelfRel()
        );
    }
}