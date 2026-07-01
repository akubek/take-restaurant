package pl.polsl.take.restaurant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import pl.polsl.take.restaurant.model.enums.Allergen;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;
import pl.polsl.take.restaurant.model.enums.Unit;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.IngredientRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for dish service rules, including recipe validation and soft-delete behavior.
 */
class DishServiceTest {

    @Mock
    private DishRepository dishRepo;

    @Mock
    private IngredientRepository ingredientRepo;

    @Mock
    private OrderRepository orderRepo;

    @InjectMocks
    private DishService dishService;

    @Captor
    private ArgumentCaptor<Dish> dishCaptor;

    @Test
    void shouldReturnDishDTOForActiveDish() {

        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);
        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));

        DishDTO result = dishService.getById(1L);

        assertNotNull(result);
        assertEquals("Pizza", result.getName());
    }

    @Test
    void shouldThrowNotFoundWhenDishDoesNotExistOrIsInactive() {
        when(dishRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> dishService.getById(99L));
    }

    @Test
    void shouldCreateDishWithoutIngredients() {
        // Given: creation payload without recipe items
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Cola");
        dto.setPriceInCents(800);

        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> {
            Dish d = i.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 1L);
            return d;
        });

        // When: dish is created
        DishDTO result = dishService.create(dto);

        // Then: saved dish has empty recipe and valid response
        verify(dishRepo).save(dishCaptor.capture());
        Dish saved = dishCaptor.getValue();

        assertEquals("Cola", saved.getName());
        assertTrue(saved.getRecipeItems().isEmpty());
        assertNotNull(result);
    }

    @Test
    void shouldCreateDishAndLinkExistingIngredient() {
        // Given: existing ingredient referenced by ingredientId
        Ingredient existingIngredient = new Ingredient("Ser", false, Unit.GRAM, Set.of());
        ReflectionTestUtils.setField(existingIngredient, "id", 5L);

        when(ingredientRepo.findById(5L)).thenReturn(Optional.of(existingIngredient));
        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> i.getArgument(0));

        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredientId(5L);
        itemReq.setAmount(150.0);

        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza Cheese");
        dto.setIngredients(List.of(itemReq));

        // When: dish is created
        dishService.create(dto);

        // Then: recipe links existing ingredient and no new ingredient is saved
        verify(dishRepo).save(dishCaptor.capture());
        Dish saved = dishCaptor.getValue();

        assertEquals(1, saved.getRecipeItems().size());
        assertEquals(existingIngredient, saved.getRecipeItems().get(0).getIngredient());
        assertEquals(150.0, saved.getRecipeItems().get(0).getAmount());

        verify(ingredientRepo, never()).save(any());
    }

    @Test
    void shouldCreateDishAndCreateNewIngredientOnTheFly() {
        // Given: recipe item carries inline ingredient object
        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(i -> {
            Ingredient ing = i.getArgument(0);
            ReflectionTestUtils.setField(ing, "id", 10L);
            return ing;
        });
        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> i.getArgument(0));

        CreateIngredientDTO newIngDto = new CreateIngredientDTO();
        newIngDto.setName("Tajny Sos");
        newIngDto.setUnit(Unit.MILLILITER);

        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredient(newIngDto);
        itemReq.setAmount(50.0);

        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Burger");
        dto.setIngredients(List.of(itemReq));

        // When: dish is created
        dishService.create(dto);

        // Then: inline ingredient is created and attached to saved dish recipe
        verify(ingredientRepo).save(any(Ingredient.class));
        verify(dishRepo).save(dishCaptor.capture());

        Dish savedDish = dishCaptor.getValue();
        assertEquals(1, savedDish.getRecipeItems().size());
        assertEquals("Tajny Sos", savedDish.getRecipeItems().get(0).getIngredient().getName());
    }

    @Test
    void shouldThrowConflictWhenBothIngredientIdAndObjectAreProvided() {

        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredientId(5L);
        itemReq.setIngredient(new CreateIngredientDTO());

        CreateDishDTO dto = new CreateDishDTO();
        dto.setIngredients(List.of(itemReq));

        assertThrows(ConflictException.class, () -> dishService.create(dto));
        verify(dishRepo, never()).save(any());
    }

    @Test
    void shouldThrowConflictWhenNeitherIngredientIdNorObjectAreProvided() {

        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredientId(null);
        itemReq.setIngredient(null);

        CreateDishDTO dto = new CreateDishDTO();
        dto.setIngredients(List.of(itemReq));

        assertThrows(ConflictException.class, () -> dishService.create(dto));
    }

    @Test
    void shouldUpdateDishProperties() {
        // Given: existing active dish and update payload
        Dish dish = new Dish("Stara", "Opis", 1000, 500, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));
        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> i.getArgument(0));

        UpdateDishDTO updateDto = new UpdateDishDTO();
        updateDto.setName("Nowa");
        updateDto.setDescription("Nowy Opis");
        updateDto.setPriceInCents(2000);

        // When: update is executed
        DishDTO result = dishService.update(1L, updateDto);

        // Then: dish fields are updated in entity and response
        assertEquals("Nowa", result.getName());
        assertEquals(2000, result.getPriceInCents());
        assertEquals("Nowa", dish.getName());
    }

    @Test
    void shouldHardDeleteDishWhenNeverOrdered() {
        // Given: dish exists and has no order history
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));

        when(orderRepo.existsByOrderItemsDishId(1L)).thenReturn(false);

        // When: delete is requested
        dishService.delete(1L);

        // Then: hard delete is executed
        verify(dishRepo).delete(dish);
        verify(dishRepo, never()).save(any());
    }

    @Test
    void shouldSoftDeleteDishWhenAlreadyOrdered() {
        // Given: dish exists and has order history
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));

        when(orderRepo.existsByOrderItemsDishId(1L)).thenReturn(true);

        // When: delete is requested
        dishService.delete(1L);

        // Then: dish is soft-deactivated instead of hard deleted
        assertFalse(dish.getIsActive());
        verify(dishRepo).save(dish);
        verify(dishRepo, never()).delete(any());
    }

    @Test
    void shouldDeactivateDish() {

        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        when(dishRepo.findById(1L)).thenReturn(Optional.of(dish));
        when(dishRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        dishService.deactivateDish(1L);

        assertFalse(dish.getIsActive());
        verify(dishRepo).save(dish);
    }

    @Test
    void shouldReactivateDish() {

        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        dish.setIsActive(false);
        when(dishRepo.findById(1L)).thenReturn(Optional.of(dish));
        when(dishRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        dishService.reactivateDish(1L);

        assertTrue(dish.getIsActive());
        verify(dishRepo).save(dish);
    }

    @Test
    void shouldReturnRecipeItemsForActiveDish() {
        // Given: active dish with one recipe item
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        Ingredient ingredient = new Ingredient("Ser", false, Unit.GRAM, Set.of());
        RecipeItem recipeItem = new RecipeItem(dish, ingredient, 150.0);
        dish.getRecipeItems().add(recipeItem);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));

        // When: recipe items are fetched
        List<RecipeItemResponseDTO> result = dishService.getIngredients(1L);

        // Then: mapped recipe item data is returned
        assertEquals(1, result.size());
        assertEquals("Ser", result.get(0).getIngredientName());
        assertEquals(150.0, result.get(0).getAmount());
        assertEquals("GRAM", result.get(0).getUnit());
        assertFalse(result.get(0).getIsVegan());
    }

    @Test
    void shouldReturnEmptyListForDishWithNoRecipe() {

        Dish dish = new Dish("Cola", "Napój", 800, 150, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 2L);

        when(dishRepo.findByIdAndIsActiveTrue(2L)).thenReturn(Optional.of(dish));

        List<RecipeItemResponseDTO> result = dishService.getIngredients(2L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowNotFoundWhenGettingIngredientsForNonExistentDish() {
        when(dishRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> dishService.getIngredients(99L));
    }

    @Test
    void shouldReturnMultipleRecipeItemsForDish() {
        // Given: active dish with multiple recipe entries
        Dish dish = new Dish("Burger", "Opis", 2500, 700, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 3L);

        Ingredient bun = new Ingredient("Bułka", true, Unit.GRAM, Set.of(Allergen.GLUTEN));
        Ingredient meat = new Ingredient("Mięso", false, Unit.GRAM, Set.of());
        dish.getRecipeItems().add(new RecipeItem(dish, bun, 80.0));
        dish.getRecipeItems().add(new RecipeItem(dish, meat, 200.0));

        when(dishRepo.findByIdAndIsActiveTrue(3L)).thenReturn(Optional.of(dish));

        // When: recipe items are fetched
        List<RecipeItemResponseDTO> result = dishService.getIngredients(3L);

        // Then: all recipe items are returned
        assertEquals(2, result.size());
    }
}