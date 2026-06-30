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

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepo;
    private final RecipeItemRepository recipeItemRepo;

    @Transactional(readOnly = true)
    public IngredientDTO getById(Long id) {
        return new IngredientDTO(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> getAll() {
        return ingredientRepo.findAll().stream()
                .map(IngredientDTO::new)
                .toList();
    }

    @Transactional
    public IngredientDTO create(CreateIngredientDTO dto) {
        Ingredient ingredient = new Ingredient(
                dto.getName(), dto.getIsVegan(), dto.getUnit(), dto.getAllergens()
        );
        return new IngredientDTO(ingredientRepo.save(ingredient));
    }

    @Transactional
    public IngredientDTO update(Long id, UpdateIngredientDTO dto) {
        Ingredient ingredient = findByIdOrThrow(id);
        ingredient.setName(dto.getName());
        ingredient.setIsVegan(dto.getIsVegan());
        ingredient.updateAllergens(dto.getAllergens());
        return new IngredientDTO(ingredientRepo.save(ingredient));
    }

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = findByIdOrThrow(id);

        // block if the ingredient is used in ANY recipe
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