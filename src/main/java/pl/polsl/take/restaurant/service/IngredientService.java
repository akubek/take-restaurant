package pl.polsl.take.restaurant.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.*;
import pl.polsl.take.restaurant.exception.*;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.repository.IngredientRepository;
import pl.polsl.take.restaurant.repository.RecipeItemRepository;

/**
 * Manages ingredient CRUD operations and protects referenced ingredients from deletion.
 */
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepo;
    private final RecipeItemRepository recipeItemRepo;

    /**
     * Returns ingredient by ID.
     *
     * @param id ingredient ID
     * @return ingredient DTO
     */
    @Transactional(readOnly = true)
    public IngredientDTO getById(Long id) {
        return new IngredientDTO(findByIdOrThrow(id));
    }

    /**
     * Returns all ingredients.
     *
     * @return list of ingredient DTOs
     */
    @Transactional(readOnly = true)
    public List<IngredientDTO> getAll() {
        return ingredientRepo.findAll().stream()
                .map(IngredientDTO::new)
                .toList();
    }

    /**
     * Creates a new ingredient.
     *
     * @param dto ingredient creation payload
     * @return created ingredient DTO
     */
    @Transactional
    public IngredientDTO create(CreateIngredientDTO dto) {
        Ingredient ingredient = new Ingredient(
                dto.getName(), dto.getIsVegan(), dto.getUnit(), dto.getAllergens());
        return new IngredientDTO(ingredientRepo.save(ingredient));
    }

    /**
     * Updates ingredient data.
     *
     * @param id ingredient ID
     * @param dto ingredient update payload
     * @return updated ingredient DTO
     */
    @Transactional
    public IngredientDTO update(Long id, UpdateIngredientDTO dto) {
        Ingredient ingredient = findByIdOrThrow(id);
        ingredient.setName(dto.getName());
        ingredient.setIsVegan(dto.getIsVegan());
        ingredient.updateAllergens(dto.getAllergens());
        return new IngredientDTO(ingredientRepo.save(ingredient));
    }

    /**
     * Deletes an ingredient only when it is not referenced by any recipe item.
     *
     * @param id ingredient ID
     */
    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = findByIdOrThrow(id);

        if (recipeItemRepo.existsByIngredientId(id)) {
            throw new ConflictException("Cannot delete ingredient because it is used in an existing recipe.");
        }

        ingredientRepo.delete(ingredient);
    }

    private Ingredient findByIdOrThrow(Long id) {
        return ingredientRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient with ID " + id + " does not exist."));
    }
}