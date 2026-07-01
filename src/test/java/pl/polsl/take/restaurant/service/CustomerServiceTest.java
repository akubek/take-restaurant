package pl.polsl.take.restaurant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pl.polsl.take.restaurant.dto.CreateCustomerDTO;
import pl.polsl.take.restaurant.dto.CustomerDTO;
import pl.polsl.take.restaurant.dto.UpdateCustomerDTO;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for customer service behavior, including active filtering and anonymization.
 */
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private OrderRepository orderRepo;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldReturnCustomerDTOForActiveCustomer() {
        // Given: existing active customer in repository
        Customer customer = new Customer("Jan", "Kowalski", "123456789", "jan@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));

        // When: customer is fetched by ID
        CustomerDTO result = customerService.getById(1L);

        // Then: mapped DTO contains expected customer data
        assertNotNull(result);
        assertEquals("Jan", result.getFirstName());
        assertEquals("Kowalski", result.getLastName());
    }

    @Test
    void shouldThrowNotFoundForNonExistentOrInactiveCustomer() {
        // Given: repository has no active customer for requested ID
        when(customerRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        // When / Then: lookup fails with not-found exception
        assertThrows(NotFoundException.class, () -> customerService.getById(99L));
    }

    @Test
    void shouldReturnOnlyActiveCustomers() {
        // Given: repository returns only active records
        Customer active = new Customer("Anna", "Nowak", "987", "anna@test.com");
        when(customerRepo.findAllByIsActiveTrue()).thenReturn(List.of(active));

        // When: all customers are requested
        List<CustomerDTO> result = customerService.getAll();

        // Then: only active customers are returned and correct repo method is used
        assertEquals(1, result.size());

        verify(customerRepo).findAllByIsActiveTrue();
        verify(customerRepo, never()).findAll();
    }

    @Test
    void shouldCreateAnonymousCustomerWithoutPersonalData() {
        // Given: creation payload without personal fields
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setFirstName(null);
        dto.setLastName(null);
        dto.setPhoneNumber(null);
        dto.setEmail(null);

        Customer saved = new Customer(null, null, null, null);
        ReflectionTestUtils.setField(saved, "id", 5L);
        when(customerRepo.save(any())).thenReturn(saved);

        // When: customer is created
        CustomerDTO result = customerService.create(dto);

        // Then: creation succeeds and entity is persisted
        assertNotNull(result);
        verify(customerRepo).save(any(Customer.class));
    }

    @Test
    void shouldAnonymizeCustomerDataAndSetInactive() {
        // Given: active customer exists
        Customer customer = new Customer("Jan", "Kowalski", "123456789", "jan@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(customerRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When: anonymization is executed
        customerService.anonymize(1L);

        // Then: personal data is removed and customer is marked inactive
        assertFalse(customer.isActive());
        assertNull(customer.getPhoneNumber());
        assertNull(customer.getEmail());
        verify(customerRepo).save(customer);
    }

    @Test
    void shouldThrowWhenAnonymizingNonExistentCustomer() {
        when(customerRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> customerService.anonymize(99L));
        verify(customerRepo, never()).save(any());
    }

    @Test
    void shouldReturnZeroForCustomerWithNoOrders() {
        // Given: active customer with no paid orders
        Customer customer = new Customer("Jan", "Kowalski", "123", "jan@test.com");
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(orderRepo.sumCustomerSpending(1L)).thenReturn(Optional.empty());

        // When: spending is requested
        Long result = customerService.getTotalSpending(1L);

        // Then: spending defaults to zero
        assertEquals(0L, result);
    }

    @Test
    void shouldReturnCorrectTotalSpending() {
        // Given: active customer with aggregated spending value
        Customer customer = new Customer("Jan", "Kowalski", "123", "jan@test.com");
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(orderRepo.sumCustomerSpending(1L)).thenReturn(Optional.of(15000L));

        // When: spending is requested
        Long result = customerService.getTotalSpending(1L);

        // Then: returned value matches repository aggregation
        assertEquals(15000L, result);
    }

    @Test
    void shouldThrowNotFoundWhenGettingSpendingForNonExistentCustomer() {
        // Given: customer does not exist as active
        when(customerRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        // When / Then: spending request fails before order query is executed
        assertThrows(NotFoundException.class, () -> customerService.getTotalSpending(99L));

        verify(orderRepo, never()).sumCustomerSpending(any());
    }

    @Test
    void shouldUpdateCustomerFields() {
        // Given: existing active customer and update payload
        Customer customer = new Customer("Jan", "Kowalski", "111", "old@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(customerRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setFirstName("Piotr");
        dto.setLastName("Nowak");
        dto.setPhoneNumber("999999999");
        dto.setEmail("new@test.com");

        // When: update is executed
        CustomerDTO result = customerService.update(1L, dto);

        // Then: DTO and entity reflect updated values
        assertEquals("Piotr", result.getFirstName());
        assertEquals("Nowak", result.getLastName());
    }
}