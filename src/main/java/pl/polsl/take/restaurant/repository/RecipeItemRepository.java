package pl.polsl.take.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.polsl.take.restaurant.model.RecipeItem;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {

    boolean existsByIngredientId(Long ingredientId);

    boolean existsByDishId(Long dishId);
}
