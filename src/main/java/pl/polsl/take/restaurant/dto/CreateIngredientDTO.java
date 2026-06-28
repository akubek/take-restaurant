package pl.polsl.take.restaurant.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.Unit;
import pl.polsl.take.restaurant.model.enums.Allergen;

@Getter
@Setter
public class CreateIngredientDTO {
    @NotBlank
    private String name;

    @NotNull
    private Boolean isVegan;

    @NotNull
    private Unit unit;

    private Set<Allergen> allergens;
}
