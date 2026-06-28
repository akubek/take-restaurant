package pl.polsl.take.restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.polsl.take.restaurant.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByIsActiveTrue();
    Optional<Customer> findByIdAndIsActiveTrue(Long id);
}
