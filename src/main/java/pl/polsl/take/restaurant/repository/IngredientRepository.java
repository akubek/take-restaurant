package pl.polsl.take.restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.polsl.take.restaurant.model.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByIsActiveTrue();
    Optional<Ingredient> findByIdAndIsActiveTrue(Long id);
}