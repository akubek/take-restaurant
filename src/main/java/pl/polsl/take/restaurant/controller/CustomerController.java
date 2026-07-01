package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Customers", description = "Operations for managing restaurant customers")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Returns a single active customer by identifier")
    public CustomerDTO get(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Get all customers", description = "Returns all active customers")
    public CollectionModel<CustomerDTO> getAll() {
        List<CustomerDTO> customers = customerService.getAll();
        return CollectionModel.of(customers, linkTo(methodOn(CustomerController.class).getAll()).withSelfRel());
    }

    @PostMapping
    @Operation(summary = "Create customer", description = "Creates a new customer")
    public CustomerDTO create(@Valid @RequestBody CreateCustomerDTO dto) {
        return customerService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Updates customer data by identifier")
    public CustomerDTO update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerDTO dto) {
        return customerService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Anonymize customer", description = "Performs soft delete by anonymizing personal data")
    public ResponseEntity<Void> anonymize(@PathVariable Long id) {
        customerService.anonymize(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/spending")
    @Operation(summary = "Get customer spending", description = "Returns total amount spent by the selected customer")
    public Long getSpending(@PathVariable Long id) {
        return customerService.getTotalSpending(id);
    }

    @GetMapping("/{id}/orders")
    @Operation(summary = "Get customer orders", description = "Returns all orders assigned to the selected customer")
    public CollectionModel<OrderDTO> orders(@PathVariable Long id) {
        List<OrderDTO> orders = orderService.getByCustomerId(id);

        return CollectionModel.of(
                orders,
                linkTo(methodOn(CustomerController.class).orders(id)).withSelfRel());
    }
}