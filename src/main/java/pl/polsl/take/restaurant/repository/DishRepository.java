package pl.polsl.take.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.polsl.take.restaurant.model.Dish;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findAllByIsActiveTrue();
}