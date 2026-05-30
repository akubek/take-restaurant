package pl.polsl.take.restaurant.model.DTOs;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.RepresentationModel;

import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.controller.CustomerController;
import pl.polsl.take.restaurant.model.Customer;

@Getter
public class CustomerDTO extends RepresentationModel<CustomerDTO> {

    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;

    public CustomerDTO(Customer customer) {
        this.id = customer.getId();
        this.email = customer.getEmail();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.phoneNumber = customer.getPhoneNumber();

        add(linkTo(methodOn(CustomerController.class)
                .get(customer.getId()))
                .withSelfRel());

        add(linkTo(methodOn(CustomerController.class)
                .orders(customer.getId()))
                .withRel("orders"));
    }
}
