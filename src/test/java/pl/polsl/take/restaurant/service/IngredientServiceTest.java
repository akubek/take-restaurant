package pl.polsl.take.restaurant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pl.polsl.take.restaurant.dto.CreateIngredientDTO;
import pl.polsl.take.restaurant.dto.IngredientDTO;
import pl.polsl.take.restaurant.dto.UpdateIngredientDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.model.enums.Allergen;
import pl.polsl.take.restaurant.model.enums.Unit;
import pl.polsl.take.restaurant.repository.IngredientRepository;
import pl.polsl.take.restaurant.repository.RecipeItemRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for ingredient service CRUD logic and recipe-reference deletion guard.
 */
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepo;

    @Mock
    private RecipeItemRepository recipeItemRepo;

    @InjectMocks
    private IngredientService ingredientService;

    @Captor
    private ArgumentCaptor<Ingredient> ingredientCaptor;

    @Test
    void shouldReturnIngredientDTOForExistingIngredient() {

        Ingredient ingredient = new Ingredient("Mąka", true, Unit.GRAM, Set.of(Allergen.GLUTEN));
        ReflectionTestUtils.setField(ingredient, "id", 1L);
        when(ingredientRepo.findById(1L)).thenReturn(Optional.of(ingredient));

        IngredientDTO result = ingredientService.getById(1L);

        assertNotNull(result);
        assertEquals("Mąka", result.getName());
        assertTrue(result.getIsVegan());
        assertEquals("GRAM", result.getUnit());
        assertTrue(result.getAllergens().contains(Allergen.GLUTEN));
    }

    @Test
    void shouldThrowNotFoundWhenIngredientDoesNotExist() {
        when(ingredientRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ingredientService.getById(99L));
    }

    @Test
    void shouldReturnAllIngredients() {

        Ingredient i1 = new Ingredient("Mąka", true, Unit.GRAM, Set.of());
        Ingredient i2 = new Ingredient("Mleko", false, Unit.MILLILITER, Set.of(Allergen.LACTOSE));
        when(ingredientRepo.findAll()).thenReturn(List.of(i1, i2));

        List<IngredientDTO> result = ingredientService.getAll();

        assertEquals(2, result.size());
        verify(ingredientRepo).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoIngredients() {
        when(ingredientRepo.findAll()).thenReturn(List.of());

        List<IngredientDTO> result = ingredientService.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCreateIngredientWithAllergens() {
        // Given: creation payload with allergen metadata
        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("Pszenica");
        dto.setIsVegan(true);
        dto.setUnit(Unit.GRAM);
        dto.setAllergens(Set.of(Allergen.GLUTEN));

        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(i -> {
            Ingredient saved = i.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // When: ingredient is created
        IngredientDTO result = ingredientService.create(dto);

        // Then: persisted ingredient and DTO carry expected values
        verify(ingredientRepo).save(ingredientCaptor.capture());
        Ingredient saved = ingredientCaptor.getValue();
        assertEquals("Pszenica", saved.getName());
        assertTrue(saved.getIsVegan());
        assertEquals(Unit.GRAM, saved.getUnit());
        assertTrue(saved.getAllergens().contains(Allergen.GLUTEN));
        assertNotNull(result);
    }

    @Test
    void shouldCreateIngredientWithoutAllergens() {
        // Given: creation payload without allergens
        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("Oliwa");
        dto.setIsVegan(true);
        dto.setUnit(Unit.MILLILITER);
        dto.setAllergens(null);

        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(i -> i.getArgument(0));

        // When: ingredient is created
        IngredientDTO result = ingredientService.create(dto);

        // Then: allergen set is empty and result is returned
        verify(ingredientRepo).save(ingredientCaptor.capture());
        assertTrue(ingredientCaptor.getValue().getAllergens().isEmpty());
        assertNotNull(result);
    }

    @Test
    void shouldCreateNonVeganIngredient() {

        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("Ser");
        dto.setIsVegan(false);
        dto.setUnit(Unit.GRAM);
        dto.setAllergens(Set.of(Allergen.LACTOSE));

        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(i -> i.getArgument(0));

        ingredientService.create(dto);

        verify(ingredientRepo).save(ingredientCaptor.capture());
        assertFalse(ingredientCaptor.getValue().getIsVegan());
    }

    @Test
    void shouldUpdateIngredientNameAndVeganStatus() {
        // Given: existing ingredient and update payload with new allergens
        Ingredient ingredient = new Ingredient("Stara Nazwa", false, Unit.GRAM, new HashSet<>());
        ReflectionTestUtils.setField(ingredient, "id", 1L);
        when(ingredientRepo.findById(1L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(i -> i.getArgument(0));

        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName("Nowa Nazwa");
        dto.setIsVegan(true);
        dto.setAllergens(Set.of(Allergen.PEANUTS));

        // When: update is executed
        IngredientDTO result = ingredientService.update(1L, dto);

        // Then: entity and result reflect updated state
        assertEquals("Nowa Nazwa", ingredient.getName());
        assertTrue(ingredient.getIsVegan());
        assertTrue(ingredient.getAllergens().contains(Allergen.PEANUTS));
        assertEquals("Nowa Nazwa", result.getName());
        verify(ingredientRepo).save(ingredient);
    }

    @Test
    void shouldUpdateIngredientAndClearAllergens() {
        // Given: existing ingredient currently having allergens
        Ingredient ingredient = new Ingredient("Mleko", false, Unit.MILLILITER,
                new HashSet<>(Set.of(Allergen.LACTOSE)));
        ReflectionTestUtils.setField(ingredient, "id", 2L);
        when(ingredientRepo.findById(2L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(i -> i.getArgument(0));

        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName("Mleko Roślinne");
        dto.setIsVegan(true);
        dto.setAllergens(null);

        // When: update is executed with null allergens
        ingredientService.update(2L, dto);

        // Then: allergens are cleared
        assertTrue(ingredient.getAllergens().isEmpty());
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistentIngredient() {
        when(ingredientRepo.findById(99L)).thenReturn(Optional.empty());

        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName("Cokolwiek");
        dto.setIsVegan(true);
        dto.setAllergens(Set.of());

        assertThrows(NotFoundException.class, () -> ingredientService.update(99L, dto));
        verify(ingredientRepo, never()).save(any());
    }

    @Test
    void shouldDeleteIngredientWhenNotUsedInAnyRecipe() {
        // Given: ingredient exists and is not referenced by any recipe item
        Ingredient ingredient = new Ingredient("Sól", true, Unit.GRAM, Set.of());
        ReflectionTestUtils.setField(ingredient, "id", 1L);
        when(ingredientRepo.findById(1L)).thenReturn(Optional.of(ingredient));
        when(recipeItemRepo.existsByIngredientId(1L)).thenReturn(false);

        // When: delete is requested
        ingredientService.delete(1L);

        // Then: ingredient is deleted from repository
        verify(ingredientRepo).delete(ingredient);
        verify(ingredientRepo, never()).save(any());
    }

    @Test
    void shouldThrowConflictWhenDeletingIngredientUsedInRecipe() {
        // Given: ingredient exists and is used in recipe
        Ingredient ingredient = new Ingredient("Mąka", true, Unit.GRAM, Set.of(Allergen.GLUTEN));
        ReflectionTestUtils.setField(ingredient, "id", 1L);
        when(ingredientRepo.findById(1L)).thenReturn(Optional.of(ingredient));
        when(recipeItemRepo.existsByIngredientId(1L)).thenReturn(true);

        // When / Then: delete is blocked with conflict
        assertThrows(ConflictException.class, () -> ingredientService.delete(1L));
        verify(ingredientRepo, never()).delete(any());
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentIngredient() {
        when(ingredientRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ingredientService.delete(99L));
        verify(ingredientRepo, never()).delete(any());

        verify(recipeItemRepo, never()).existsByIngredientId(any());
    }
}
