package pl.polsl.take.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.polsl.take.restaurant.model.Ingredient;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {}