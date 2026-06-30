package pl.polsl.take.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateCustomerDTO;
import pl.polsl.take.restaurant.dto.CustomerDTO;
import pl.polsl.take.restaurant.dto.UpdateCustomerDTO;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;
import pl.polsl.take.restaurant.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final OrderRepository orderRepo;
	private final CustomerRepository customerRepo;

	@Transactional(readOnly = true)
	public CustomerDTO getById(Long id) {
		return new CustomerDTO(findActiveById(id));
	}

	@Transactional(readOnly = true)
	public List<CustomerDTO> getAll() {
		return customerRepo.findAllByIsActiveTrue()
				.stream()
				.map(CustomerDTO::new)
				.toList();
	}
	
	@Transactional
	public CustomerDTO create(CreateCustomerDTO dto)
	{
		Customer customer = new Customer(
				dto.getFirstName(), dto.getLastName(),
				dto.getPhoneNumber(), dto.getEmail());
		
		return new CustomerDTO(customerRepo.save(customer));
	}

	@Transactional
	public CustomerDTO update(Long id, UpdateCustomerDTO dto) {
		Customer customer = findActiveById(id);
		customer.setFirstName(dto.getFirstName());
		customer.setLastName(dto.getLastName());
		customer.setPhoneNumber(dto.getPhoneNumber());
		customer.setEmail(dto.getEmail());

		return new CustomerDTO(customerRepo.save(customer));
	}

	@Transactional
	public void anonymize(Long id) {
		Customer customer = findActiveById(id);
		customer.setFirstName("Anonymized");
		customer.setLastName("Removed");
		customer.setPhoneNumber(null);
		customer.setEmail(null);

		customer.setActive(false);

		customerRepo.save(customer);
	}

	@Transactional(readOnly = true)
    public Long getTotalSpending(Long id) {
        findActiveById(id);
 
        return orderRepo.sumCustomerSpending(id).orElse(0L);
    }
 
	private Customer findActiveById(Long id) {
        return customerRepo.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Customer with ID " + id + " does not exist."));
    }

}
