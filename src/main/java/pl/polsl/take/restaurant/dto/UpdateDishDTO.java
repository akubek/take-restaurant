package pl.polsl.take.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload for updating editable dish fields")
public class UpdateDishDTO {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    @Schema(description = "Dish price in minor currency units (cents/grosz)", example = "3500")
    private Integer priceInCents;
}