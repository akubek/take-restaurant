package pl.polsl.take.restaurant.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;

@Getter
@Setter
@Schema(description = "Payload for creating a menu dish")
public class CreateDishDTO {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    @Schema(description = "Dish price in minor currency units (cents/grosz)", example = "3200")
    private Integer priceInCents;

    @NotNull
    @Min(value = 0)
    private Integer calories;

    @NotNull
    @Schema(example = "MILD")
    private SpicinessLevel spiciness;

    @Valid
    @JsonProperty("ingredients")
    private List<RecipeItemRequestDTO> ingredients;
}