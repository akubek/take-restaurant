package pl.polsl.take.restaurant;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.DishRepository;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.model.IngredientRepository;
import pl.polsl.take.restaurant.model.RecipeItem;
import pl.polsl.take.restaurant.model.DTOs.CreateDishDTO;

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