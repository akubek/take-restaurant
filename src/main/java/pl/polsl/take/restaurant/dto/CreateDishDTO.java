package pl.polsl.take.restaurant.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;


@Getter
@Setter
public class CreateDishDTO {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    private Integer priceInCents;

    @NotNull
    @Min(value = 0)
    private Integer calories;

    @NotNull
    private SpicinessLevel spiciness;

    @NotEmpty
    @Valid
    private List<RecipeItemRequestDTO> ingredients;
}