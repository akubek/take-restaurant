package pl.polsl.take.restaurant.model.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RecipeItemDTO {
    private Long ingredientId;
    private Double amount;
}