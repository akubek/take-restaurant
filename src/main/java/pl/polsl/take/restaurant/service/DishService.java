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

/**
 * Handles dish management, including menu visibility and recipe-item validation.
 */
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepo;
    private final IngredientRepository ingredientRepo;
    private final OrderRepository orderRepo;

    /**
     * Returns an active dish by ID.
     *
     * @param id dish ID
     * @return dish DTO
     */
    @Transactional(readOnly = true)
    public DishDTO getById(Long id) {
        return new DishDTO(findActiveById(id));
    }

    /**
     * Returns recipe items for an active dish.
     *
     * @param dishId dish ID
     * @return list of recipe item DTOs
     */
    @Transactional(readOnly = true)
    public List<RecipeItemResponseDTO> getIngredients(Long dishId) {
        Dish dish = findActiveById(dishId);
        return dish.getRecipeItems().stream()
                .map(item -> new RecipeItemResponseDTO(item))
                .toList();
    }

    /**
     * Returns all active dishes visible in menu.
     *
     * @return list of dish DTOs
     */
    @Transactional(readOnly = true)
    public List<DishDTO> getMenu() {
        return dishRepo.findAllByIsActiveTrue()
                .stream()
                .map(DishDTO::new)
                .toList();
    }

    /**
     * Returns all dishes including inactive ones.
     *
     * @return list of dish DTOs
     */
    @Transactional(readOnly = true)
    public List<DishDTO> getAllDishes() {
        return dishRepo.findAll()
                .stream()
                .map(DishDTO::new)
                .toList();
    }

    /**
     * Creates a dish and validates recipe items so each item references either an existing
     * ingredient ID or an inline ingredient payload, but never both.
     *
     * @param dto dish creation payload
     * @return created dish DTO
     */
    @Transactional
    public DishDTO create(CreateDishDTO dto) {
        Dish dish = new Dish(
                dto.getName(),
                dto.getDescription(),
                dto.getPriceInCents(),
                dto.getCalories(),
                dto.getSpiciness());

        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {

            for (RecipeItemRequestDTO itemRequest : dto.getIngredients()) {
                Ingredient ingredient;

                boolean hasId = itemRequest.getIngredientId() != null;
                boolean hasNewObject = itemRequest.getIngredient() != null;

                if (hasId && !hasNewObject) {

                    ingredient = ingredientRepo.findById(itemRequest.getIngredientId())
                            .orElseThrow(() -> new NotFoundException("Ingredient with ID "
                                    + itemRequest.getIngredientId() + " not found or is inactive."));

                } else if (!hasId && hasNewObject) {

                    CreateIngredientDTO newIngDto = itemRequest.getIngredient();
                    ingredient = new Ingredient(
                            newIngDto.getName(),
                            newIngDto.getIsVegan(),
                            newIngDto.getUnit(),
                            newIngDto.getAllergens());
                    ingredient = ingredientRepo.save(ingredient);

                } else {

                    throw new ConflictException(
                            "Invalid recipe item: You must provide EITHER an 'ingredientId' OR a 'ingredient' object, but not both or neither.");
                }

                dish.getRecipeItems().add(new RecipeItem(dish, ingredient, itemRequest.getAmount()));
            }
        }

        return new DishDTO(dishRepo.save(dish));
    }

    /**
     * Updates basic editable dish fields.
     *
     * @param id dish ID
     * @param dto dish update payload
     * @return updated dish DTO
     */
    @Transactional
    public DishDTO update(Long id, UpdateDishDTO dto) {
        Dish dish = findActiveById(id);

        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPriceInCents(dto.getPriceInCents());

        return new DishDTO(dishRepo.save(dish));
    }

    /**
     * Deletes a dish permanently only if it has never been ordered; otherwise performs soft-delete.
     *
     * @param id dish ID
     */
    @Transactional
    public void delete(Long id) {
        Dish dish = findActiveById(id);

        if (orderRepo.existsByOrderItemsDishId(id)) {

            dish.setIsActive(false);
            dishRepo.save(dish);
        } else {

            dishRepo.delete(dish);
        }
    }

    /**
     * Deactivates dish visibility in menu.
     *
     * @param id dish ID
     */
    @Transactional
    public void deactivateDish(Long id) {
        Dish dish = dishRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Dish with ID " + id + " does not exist."));
        dish.setIsActive(false);
        dishRepo.save(dish);
    }

    /**
     * Reactivates dish visibility in menu.
     *
     * @param id dish ID
     */
    @Transactional
    public void reactivateDish(Long id) {
        Dish dish = dishRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Dish with ID " + id + " does not exist."));
        dish.setIsActive(true);
        dishRepo.save(dish);
    }

    private Dish findActiveById(Long id) {
        return dishRepo.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException(
                        "Dish with ID " + id + " does not exist or has been removed from the menu."));
    }
}