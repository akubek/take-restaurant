package pl.polsl.take.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Recipe item input: provide either ingredientId or ingredient object")
public class RecipeItemRequestDTO {
    @NotNull
    private Double amount;
    @Schema(description = "Existing ingredient ID (use this OR ingredient)")
    private Long ingredientId;
    @Schema(description = "Inline ingredient payload for on-the-fly creation (use this OR ingredientId)")
    private CreateIngredientDTO ingredient;
}