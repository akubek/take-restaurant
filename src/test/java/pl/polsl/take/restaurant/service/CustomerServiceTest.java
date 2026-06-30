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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private OrderRepository orderRepo;

    @InjectMocks
    private CustomerService customerService;

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnCustomerDTOForActiveCustomer() {
        // Given
        Customer customer = new Customer("Jan", "Kowalski", "123456789", "jan@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Jan", result.getFirstName());
        assertEquals("Kowalski", result.getLastName());
    }

    @Test
    void shouldThrowNotFoundForNonExistentOrInactiveCustomer() {
        // Given - findByIdAndIsActiveTrue zwraca empty zarówno dla nieistniejącego
        // jak i dla isActive=false (soft-deleted) klienta
        when(customerRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> customerService.getById(99L));
    }

    // -------------------------------------------------------------------------
    // getAll
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnOnlyActiveCustomers() {
        // Given - repo zwraca tylko aktywnych (findAllByIsActiveTrue)
        Customer active = new Customer("Anna", "Nowak", "987", "anna@test.com");
        when(customerRepo.findAllByIsActiveTrue()).thenReturn(List.of(active));

        // When
        List<CustomerDTO> result = customerService.getAll();

        // Then
        assertEquals(1, result.size());
        // weryfikacja że wywołano właściwą metodę (a nie findAll)
        verify(customerRepo).findAllByIsActiveTrue();
        verify(customerRepo, never()).findAll();
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateAnonymousCustomerWithoutPersonalData() {
        // Given - klient anonimowy: firstName i lastName są null
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setFirstName(null);
        dto.setLastName(null);
        dto.setPhoneNumber(null);
        dto.setEmail(null);

        Customer saved = new Customer(null, null, null, null);
        ReflectionTestUtils.setField(saved, "id", 5L);
        when(customerRepo.save(any())).thenReturn(saved);

        // When
        CustomerDTO result = customerService.create(dto);

        // Then - tworzenie nie rzuca wyjątku, klient zapisany
        assertNotNull(result);
        verify(customerRepo).save(any(Customer.class));
    }

    // -------------------------------------------------------------------------
    // anonymize
    // -------------------------------------------------------------------------

    @Test
    void shouldAnonymizeCustomerDataAndSetInactive() {
        // Given
        Customer customer = new Customer("Jan", "Kowalski", "123456789", "jan@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(customerRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        customerService.anonymize(1L);

        // Then - klient nieaktywny i dane osobowe wyczyszczone
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

    // -------------------------------------------------------------------------
    // getTotalSpending
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnZeroForCustomerWithNoOrders() {
        // Given - klient istnieje, ale nie ma zamówień (SUM zwraca NULL → Optional.empty)
        Customer customer = new Customer("Jan", "Kowalski", "123", "jan@test.com");
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(orderRepo.sumCustomerSpending(1L)).thenReturn(Optional.empty());

        // When
        Long result = customerService.getTotalSpending(1L);

        // Then - 0L zamiast NPE
        assertEquals(0L, result);
    }

    @Test
    void shouldReturnCorrectTotalSpending() {
        // Given
        Customer customer = new Customer("Jan", "Kowalski", "123", "jan@test.com");
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(orderRepo.sumCustomerSpending(1L)).thenReturn(Optional.of(15000L));

        // When
        Long result = customerService.getTotalSpending(1L);

        // Then
        assertEquals(15000L, result);
    }

    @Test
    void shouldThrowNotFoundWhenGettingSpendingForNonExistentCustomer() {
        // Given - klient nie istnieje - NIE zwracamy 0, rzucamy 404
        // (nie chcemy zwracać 0 dla id które nie istnieje w bazie)
        when(customerRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> customerService.getTotalSpending(99L));
        // weryfikacja że nigdy nie odpytano bazy o zamówienia dla nieistniejącego klienta
        verify(orderRepo, never()).sumCustomerSpending(any());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateCustomerFields() {
        // Given
        Customer customer = new Customer("Jan", "Kowalski", "111", "old@test.com");
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(customer));
        when(customerRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setFirstName("Piotr");
        dto.setLastName("Nowak");
        dto.setPhoneNumber("999999999");
        dto.setEmail("new@test.com");

        // When
        CustomerDTO result = customerService.update(1L, dto);

        // Then
        assertEquals("Piotr", result.getFirstName());
        assertEquals("Nowak", result.getLastName());
    }
}