package pl.polsl.take.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RecipeItemRequestDTO {
    @NotNull
    private Double amount;
    private Long ingredientId;
    private CreateIngredientDTO ingredient;
}