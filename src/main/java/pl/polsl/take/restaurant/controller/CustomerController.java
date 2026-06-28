package pl.polsl.take.restaurant.controller;

import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateCustomerDTO;
import pl.polsl.take.restaurant.dto.CustomerDTO;
import pl.polsl.take.restaurant.dto.OrderDTO;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.enums.OrderItemStatus;
import pl.polsl.take.restaurant.service.CustomerService;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService service;
    @PostMapping
    public CustomerDTO create(@RequestBody CreateCustomerDTO dto) {
        return new CustomerDTO(service.createCustomer(dto));
    }
    
    @GetMapping("/{id}")
    public CustomerDTO get(@PathVariable Long id) {
        return new CustomerDTO(service.getById(id));
    }
    
    @GetMapping("/{id}/orders")
    public List<OrderDTO> orders(@PathVariable Long id) {
        return service.getById(id)
                .getOrders()
                .stream()
                .map(OrderDTO::new)
                .toList();
    }
    
    @GetMapping
    public CollectionModel<CustomerDTO> getAll() {

        List<CustomerDTO> orders = service.getAll()
                .stream()
                .map(CustomerDTO::new)
                .toList();

        return CollectionModel.of(
                orders,
                linkTo(methodOn(CustomerController.class)
                        .getAll())
                        .withSelfRel()
        );
    }
}
