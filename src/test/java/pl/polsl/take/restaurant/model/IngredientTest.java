package pl.polsl.take.restaurant.model;

import org.junit.jupiter.api.Test;

import pl.polsl.take.restaurant.model.enums.Allergen;
import pl.polsl.take.restaurant.model.enums.Unit;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for ingredient entity helper behavior.
 */
class IngredientTest {

    @Test
    void shouldClearAllergensWhenUpdatingWithNull() {

        Set<Allergen> initialAllergens = new HashSet<>();
        initialAllergens.add(Allergen.LACTOSE);
        Ingredient ingredient = new Ingredient("Milk", false, Unit.LITER, initialAllergens);

        ingredient.updateAllergens(null);

        assertTrue(ingredient.getAllergens().isEmpty(), "Allergens list should be empty");
    }
}