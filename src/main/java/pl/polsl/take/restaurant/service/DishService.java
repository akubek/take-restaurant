package pl.polsl.take.restaurant.service;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateDishDTO;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.model.RecipeItem;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.IngredientRepository;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepo;
    private final IngredientRepository ingredientRepo;

    public Dish getById(Long id) {
        return dishRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Dish not found"));
    }

    public Dish createDish(CreateDishDTO dto) {
        Dish dish = new Dish(
                dto.getName(),
                dto.getDescription(),
                dto.getPriceInCents(),
                dto.getCalories(),
                dto.getSpiciness()
        );

        dto.getIngredients().forEach(i -> {
            Ingredient ing = ingredientRepo.findById(i.getIngredientId())
                    .orElseThrow();

            dish.getRecipeItems().add(
                    new RecipeItem(dish, ing, i.getAmount())
            );
        });

        return dishRepo.save(dish);
    }

    public List<Dish> getAll() {
        return dishRepo.findAll();
    }
}