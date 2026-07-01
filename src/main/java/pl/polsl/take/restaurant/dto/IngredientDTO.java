package pl.polsl.take.restaurant.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import pl.polsl.take.restaurant.model.Ingredient;
import pl.polsl.take.restaurant.model.enums.Allergen;

@Getter
@Schema(description = "Ingredient response model")
public class IngredientDTO {

    private Long id;
    private String name;
    private Boolean isVegan;
    @Schema(description = "Unit name serialized as enum text", example = "GRAM")
    private String unit;
    @JsonProperty("allergens")
    private Set<Allergen> allergens;

    public IngredientDTO(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.isVegan = ingredient.getIsVegan();
        this.unit = ingredient.getUnit().name();
        this.allergens = ingredient.getAllergens();
    }
}