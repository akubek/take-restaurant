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

    // -------------------------------------------------------------------------
    // getById / findActiveById
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnDishDTOForActiveDish() {
        // Given
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);
        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));

        // When
        DishDTO result = dishService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Pizza", result.getName());
    }

    @Test
    void shouldThrowNotFoundWhenDishDoesNotExistOrIsInactive() {
        when(dishRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> dishService.getById(99L));
    }

    // -------------------------------------------------------------------------
    // create - logika przypisywania składników (RecipeItems)
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateDishWithoutIngredients() {
        // Given - Puste DTO składników (np. dla napojów z puszki)
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Cola");
        dto.setPriceInCents(800);
        
        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> {
            Dish d = i.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 1L);
            return d;
        });

        // When
        DishDTO result = dishService.create(dto);

        // Then
        verify(dishRepo).save(dishCaptor.capture());
        Dish saved = dishCaptor.getValue();
        
        assertEquals("Cola", saved.getName());
        assertTrue(saved.getRecipeItems().isEmpty()); // Lista przepisów pusta
        assertNotNull(result);
    }

    @Test
    void shouldCreateDishAndLinkExistingIngredient() {
        // Given
        Ingredient existingIngredient = new Ingredient("Ser", false, Unit.GRAM, Set.of());
        ReflectionTestUtils.setField(existingIngredient, "id", 5L);

        when(ingredientRepo.findById(5L)).thenReturn(Optional.of(existingIngredient));
        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> i.getArgument(0));

        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredientId(5L); // Używamy ID istniejącego składnika
        itemReq.setAmount(150.0);

        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza Cheese");
        dto.setIngredients(List.of(itemReq));

        // When
        dishService.create(dto);

        // Then
        verify(dishRepo).save(dishCaptor.capture());
        Dish saved = dishCaptor.getValue();

        assertEquals(1, saved.getRecipeItems().size());
        assertEquals(existingIngredient, saved.getRecipeItems().get(0).getIngredient());
        assertEquals(150.0, saved.getRecipeItems().get(0).getAmount());
        // Upewniamy się, że nie próbowano stworzyć nowego składnika w bazie
        verify(ingredientRepo, never()).save(any());
    }

    @Test
    void shouldCreateDishAndCreateNewIngredientOnTheFly() {
        // Given - Zapis nowego składnika i zapis nowego dania
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
        itemReq.setIngredient(newIngDto); // Używamy NOWEGO obiektu zamiast ID
        itemReq.setAmount(50.0);

        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Burger");
        dto.setIngredients(List.of(itemReq));

        // When
        dishService.create(dto);

        // Then
        verify(ingredientRepo).save(any(Ingredient.class)); // Serwis musiał zapisać nowy składnik
        verify(dishRepo).save(dishCaptor.capture());
        
        Dish savedDish = dishCaptor.getValue();
        assertEquals(1, savedDish.getRecipeItems().size());
        assertEquals("Tajny Sos", savedDish.getRecipeItems().get(0).getIngredient().getName());
    }

    @Test
    void shouldThrowConflictWhenBothIngredientIdAndObjectAreProvided() {
        // Given
        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredientId(5L); // ID jest
        itemReq.setIngredient(new CreateIngredientDTO()); // Obiekt też jest -> KONFLIKT

        CreateDishDTO dto = new CreateDishDTO();
        dto.setIngredients(List.of(itemReq));

        // When / Then
        assertThrows(ConflictException.class, () -> dishService.create(dto));
        verify(dishRepo, never()).save(any());
    }

    @Test
    void shouldThrowConflictWhenNeitherIngredientIdNorObjectAreProvided() {
        // Given
        RecipeItemRequestDTO itemReq = new RecipeItemRequestDTO();
        itemReq.setIngredientId(null);
        itemReq.setIngredient(null);

        CreateDishDTO dto = new CreateDishDTO();
        dto.setIngredients(List.of(itemReq));

        // When / Then
        assertThrows(ConflictException.class, () -> dishService.create(dto));
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateDishProperties() {
        // Given
        Dish dish = new Dish("Stara", "Opis", 1000, 500, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));
        when(dishRepo.save(any(Dish.class))).thenAnswer(i -> i.getArgument(0));

        UpdateDishDTO updateDto = new UpdateDishDTO();
        updateDto.setName("Nowa");
        updateDto.setDescription("Nowy Opis");
        updateDto.setPriceInCents(2000);

        // When
        DishDTO result = dishService.update(1L, updateDto);

        // Then
        assertEquals("Nowa", result.getName());
        assertEquals(2000, result.getPriceInCents());
        assertEquals("Nowa", dish.getName()); // Upewniamy się, że encja została zaktualizowana
    }

    // -------------------------------------------------------------------------
    // delete (Soft Delete vs Hard Delete)
    // -------------------------------------------------------------------------

    @Test
    void shouldHardDeleteDishWhenNeverOrdered() {
        // Given
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));
        // Repozytorium zamówień mówi: "Nikt nigdy nie zamówił tego dania"
        when(orderRepo.existsByOrderItemsDishId(1L)).thenReturn(false);

        // When
        dishService.delete(1L);

        // Then - bezpiecznie i bezpowrotnie usuwamy z bazy
        verify(dishRepo).delete(dish);
        verify(dishRepo, never()).save(any());
    }

    @Test
    void shouldSoftDeleteDishWhenAlreadyOrdered() {
        // Given
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));
        // Repozytorium zamówień mówi: "Ktoś już to kiedyś zamówił!"
        when(orderRepo.existsByOrderItemsDishId(1L)).thenReturn(true);

        // When
        dishService.delete(1L);

        // Then - nie możemy usunąć, zmieniamy tylko isActive na false (Soft Delete)
        assertFalse(dish.getIsActive());
        verify(dishRepo).save(dish);
        verify(dishRepo, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // deactivate / reactivate
    // -------------------------------------------------------------------------

    @Test
    void shouldDeactivateDish() {
        // Given - uzywamy zwyklego findById (bo możemy chcieć operować też na nieaktywnych)
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        when(dishRepo.findById(1L)).thenReturn(Optional.of(dish));
        when(dishRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        dishService.deactivateDish(1L);

        // Then
        assertFalse(dish.getIsActive());
        verify(dishRepo).save(dish);
    }

    @Test
    void shouldReactivateDish() {
        // Given
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        dish.setIsActive(false);
        when(dishRepo.findById(1L)).thenReturn(Optional.of(dish));
        when(dishRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        dishService.reactivateDish(1L);

        // Then
        assertTrue(dish.getIsActive());
        verify(dishRepo).save(dish);
    }

    // -------------------------------------------------------------------------
    // getIngredients (RecipeItem)
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnRecipeItemsForActiveDish() {
        // Given
        Dish dish = new Dish("Pizza", "Opis", 3200, 1000, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 1L);

        Ingredient ingredient = new Ingredient("Ser", false, Unit.GRAM, Set.of());
        RecipeItem recipeItem = new RecipeItem(dish, ingredient, 150.0);
        dish.getRecipeItems().add(recipeItem);

        when(dishRepo.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(dish));

        // When
        List<RecipeItemResponseDTO> result = dishService.getIngredients(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals("Ser", result.get(0).getIngredientName());
        assertEquals(150.0, result.get(0).getAmount());
        assertEquals("GRAM", result.get(0).getUnit());
        assertFalse(result.get(0).getIsVegan());
    }

    @Test
    void shouldReturnEmptyListForDishWithNoRecipe() {
        // Given - danie bez składników w przepisie (np. napój z puszki)
        Dish dish = new Dish("Cola", "Napój", 800, 150, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 2L);

        when(dishRepo.findByIdAndIsActiveTrue(2L)).thenReturn(Optional.of(dish));

        // When
        List<RecipeItemResponseDTO> result = dishService.getIngredients(2L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowNotFoundWhenGettingIngredientsForNonExistentDish() {
        when(dishRepo.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> dishService.getIngredients(99L));
    }

    @Test
    void shouldReturnMultipleRecipeItemsForDish() {
        // Given
        Dish dish = new Dish("Burger", "Opis", 2500, 700, SpicinessLevel.MILD);
        ReflectionTestUtils.setField(dish, "id", 3L);

        Ingredient bun = new Ingredient("Bułka", true, Unit.GRAM, Set.of(Allergen.GLUTEN));
        Ingredient meat = new Ingredient("Mięso", false, Unit.GRAM, Set.of());
        dish.getRecipeItems().add(new RecipeItem(dish, bun, 80.0));
        dish.getRecipeItems().add(new RecipeItem(dish, meat, 200.0));

        when(dishRepo.findByIdAndIsActiveTrue(3L)).thenReturn(Optional.of(dish));

        // When
        List<RecipeItemResponseDTO> result = dishService.getIngredients(3L);

        // Then
        assertEquals(2, result.size());
    }
}