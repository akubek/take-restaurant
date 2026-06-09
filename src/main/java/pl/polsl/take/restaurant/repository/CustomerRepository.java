package pl.polsl.take.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.polsl.take.restaurant.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
