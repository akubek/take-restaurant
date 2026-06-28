package pl.polsl.take.restaurant.dto;

import lombok.Getter;
import pl.polsl.take.restaurant.model.RecipeItem;

@Getter
public class RecipeItemResponseDTO {
    private String ingredientName;
    private Double amount;
    private String unit;
    
    private Boolean isVegan;

    public RecipeItemResponseDTO(RecipeItem item) {
        this.ingredientName = item.getIngredient().getName();
        this.amount = item.getAmount();
        this.unit = item.getIngredient().getUnit().name();
        this.isVegan = item.getIngredient().getIsVegan();
    }
}