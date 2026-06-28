package pl.polsl.take.restaurant.dto;

import java.util.Set;
import lombok.Getter;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.model.enums.Allergen;

@Getter
public class IngredientDTO {

    private Long id;
    private String name;
    private Boolean isVegan;
    private String unit;
    private Set<Allergen> allergens;

    public IngredientDTO(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.isVegan = ingredient.getIsVegan();
        this.unit = ingredient.getUnit().name();
        this.allergens = ingredient.getAllergens();
    }
}