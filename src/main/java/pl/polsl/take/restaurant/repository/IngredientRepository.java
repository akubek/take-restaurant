package pl.polsl.take.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.polsl.take.restaurant.model.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}