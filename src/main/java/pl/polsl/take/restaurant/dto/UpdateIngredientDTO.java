package pl.polsl.take.restaurant.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.Allergen;

@Getter
@Setter
public class UpdateIngredientDTO {
    @NotBlank
    private String name;

    @NotNull
    private Boolean isVegan;

    @JsonProperty("allergens")
    private Set<Allergen> allergens;
}