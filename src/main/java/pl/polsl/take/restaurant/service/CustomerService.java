package pl.polsl.take.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateCustomerDTO;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final OrderRepository orderRepo;
	private final CustomerRepository customerRepo;

	public Customer getById(Long id) {
		return customerRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Customer not found"));
	}
	
	public Customer createCustomer(CreateCustomerDTO dto)
	{
		Customer customer = new Customer(
				dto.getFirstName(), dto.getLastName(),
				dto.getPhoneNumber(), dto.getEmail());
		
		return customerRepo.save(customer);
	}
	
	public List<Customer> getAll() {
		return customerRepo.findAll();
	}
}
