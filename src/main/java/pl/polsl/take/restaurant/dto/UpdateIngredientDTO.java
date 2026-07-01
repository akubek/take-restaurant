package pl.polsl.take.restaurant.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.Allergen;

@Getter
@Setter
@Schema(description = "Payload for updating ingredient data")
public class UpdateIngredientDTO {
    @NotBlank
    private String name;

    @NotNull
    private Boolean isVegan;

    @JsonProperty("allergens")
    @Schema(description = "Known allergens for this ingredient", example = "[\"LACTOSE\"]")
    private Set<Allergen> allergens;
}