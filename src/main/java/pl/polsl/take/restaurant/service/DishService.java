package pl.polsl.take.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateDishDTO;
import pl.polsl.take.restaurant.dto.CreateIngredientDTO;
import pl.polsl.take.restaurant.dto.DishDTO;
import pl.polsl.take.restaurant.dto.RecipeItemRequestDTO;
import pl.polsl.take.restaurant.dto.RecipeItemResponseDTO;
import pl.polsl.take.restaurant.dto.UpdateDishDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.model.RecipeItem;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.IngredientRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepo;
    private final IngredientRepository ingredientRepo;
    private final OrderRepository orderRepo;

    @Transactional(readOnly = true)
    public DishDTO getById(Long id) {
        return new DishDTO(findActiveById(id));
    }

    @Transactional(readOnly = true)
    public List<RecipeItemResponseDTO> getIngredients(Long dishId) {
        Dish dish = findActiveById(dishId);
        return dish.getRecipeItems().stream()
                .map(item -> new RecipeItemResponseDTO(item))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DishDTO> getMenu() {
        return dishRepo.findAllByIsActiveTrue()
                .stream()
                .map(DishDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DishDTO> getAllDishes() {
        return dishRepo.findAll()
                .stream()
                .map(DishDTO::new)
                .toList();
    }

    @Transactional
    public DishDTO create(CreateDishDTO dto) {
        Dish dish = new Dish(
                dto.getName(),
                dto.getDescription(),
                dto.getPriceInCents(),
                dto.getCalories(),
                dto.getSpiciness()
        );

        // Check if ingredients list is provided and not empty (allows dishes without recipes like canned drinks)
        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            
            for (RecipeItemRequestDTO itemRequest : dto.getIngredients()) {
                Ingredient ingredient;

                boolean hasId = itemRequest.getIngredientId() != null;
                boolean hasNewObject = itemRequest.getIngredient() != null;

                // Strict validation: Only ONE of the fields can be present
                if (hasId && !hasNewObject) {
                    
                    // Option 1: Link to an existing active ingredient
                    ingredient = ingredientRepo.findById(itemRequest.getIngredientId())
                            .orElseThrow(() -> new NotFoundException("Ingredient with ID " + itemRequest.getIngredientId() + " not found or is inactive."));
                            
                } else if (!hasId && hasNewObject) {
                    
                    // Option 2: Create a brand new ingredient on the fly
                    CreateIngredientDTO newIngDto = itemRequest.getIngredient();
                    ingredient = new Ingredient(
                            newIngDto.getName(),
                            newIngDto.getIsVegan(),
                            newIngDto.getUnit(),
                            newIngDto.getAllergens()
                    );
                    ingredient = ingredientRepo.save(ingredient);
                    
                } else {
                    
                    // Invalid Request: Both are null OR both are filled
                    throw new ConflictException("Invalid recipe item: You must provide EITHER an 'ingredientId' OR a 'ingredient' object, but not both or neither.");
                }

                // Add the validated recipe item to the dish
                dish.getRecipeItems().add(new RecipeItem(dish, ingredient, itemRequest.getAmount()));
            }
        }

        return new DishDTO(dishRepo.save(dish));
    }

    @Transactional
    public DishDTO update(Long id, UpdateDishDTO dto) {
        Dish dish = findActiveById(id);

        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPriceInCents(dto.getPriceInCents());

        return new DishDTO(dishRepo.save(dish));
    }

    @Transactional
    public void delete(Long id) {
        Dish dish = findActiveById(id);

        // check if the dish has ever been ordered
        if (orderRepo.existsByOrderItemsDishId(id)) {
            // if dish was ever ordered, it cannot be deleted, it's soft deleted instead - set as inactive
            dish.setIsActive(false);
            dishRepo.save(dish);
        } else {
            // if dish was never ordered, it can be safely deleted from the database
            dishRepo.delete(dish);
        }
    }

    @Transactional
    public void deactivateDish(Long id) {
        Dish dish = dishRepo.findById(id).orElseThrow(() -> new NotFoundException("Dish with ID " + id + " does not exist."));
        dish.setIsActive(false);
        dishRepo.save(dish);
    }

    @Transactional
    public void reactivateDish(Long id) {
        Dish dish = dishRepo.findById(id).orElseThrow(() -> new NotFoundException("Dish with ID " + id + " does not exist."));
        dish.setIsActive(true);
        dishRepo.save(dish);
    }

    // private method for finding active dish by ID, 
    // throws NotFoundException if not found
    private Dish findActiveById(Long id) {
        return dishRepo.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Dish with ID " + id + " does not exist or has been removed from the menu."));
    }
}