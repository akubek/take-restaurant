package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateCustomerDTO;
import pl.polsl.take.restaurant.dto.CustomerDTO;
import pl.polsl.take.restaurant.dto.OrderDTO;
import pl.polsl.take.restaurant.dto.UpdateCustomerDTO;
import pl.polsl.take.restaurant.service.CustomerService;
import pl.polsl.take.restaurant.service.OrderService;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    @GetMapping("/{id}")
    public CustomerDTO get(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping
    public CollectionModel<CustomerDTO> getAll() {
        List<CustomerDTO> customers = customerService.getAll();
        return CollectionModel.of(customers, linkTo(methodOn(CustomerController.class).getAll()).withSelfRel());
    }

    @PostMapping
    public CustomerDTO create(@Valid @RequestBody CreateCustomerDTO dto) {
        return customerService.create(dto);
    }

    @PutMapping("/{id}")
    public CustomerDTO update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerDTO dto) {
        return customerService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anonymize(@PathVariable Long id) {
        customerService.anonymize(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/spending")
    public Long getSpending(@PathVariable Long id) {
        return customerService.getTotalSpending(id);
    }

    @GetMapping("/{id}/orders")
    public CollectionModel<OrderDTO> orders(@PathVariable Long id) {
        List<OrderDTO> orders = orderService.getByCustomerId(id);

        return CollectionModel.of(
                orders, 
                linkTo(methodOn(CustomerController.class).orders(id)).withSelfRel()
        );
    }
}