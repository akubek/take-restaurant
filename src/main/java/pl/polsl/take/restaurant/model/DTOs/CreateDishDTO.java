package pl.polsl.take.restaurant.model.DTOs;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;


@Getter @Setter
public class CreateDishDTO {
    private String name;
    private String description;
    private Integer priceInCents;
    private Integer calories;
    private SpicinessLevel spiciness;

    private List<RecipeItemDTO> ingredients;
}