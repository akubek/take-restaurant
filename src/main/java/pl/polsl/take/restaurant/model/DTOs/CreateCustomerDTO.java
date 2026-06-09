package pl.polsl.take.restaurant.model.DTOs;

import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.CustomerRepository;

@Getter @Setter
public class CreateCustomerDTO {
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private CustomerRepository customerRepo;
	
	public Customer createCustomer(CreateCustomerDTO dto) {
	    Customer customer = new Customer(
	            dto.getFirstName(),
	            dto.getLastName(),
	            dto.getPhoneNumber(),
	            dto.getEmail()
	    );

	    return customerRepo.save(customer);
	}
}
