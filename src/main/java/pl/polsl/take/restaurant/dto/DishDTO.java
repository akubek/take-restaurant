package pl.polsl.take.restaurant.dto;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import pl.polsl.take.restaurant.controller.DishController;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;

@Getter
@Schema(description = "Dish response model")
public class DishDTO extends RepresentationModel<DishDTO> {

        private Long id;
        private String name;
        private String description;
        @Schema(description = "Dish price in minor currency units (cents/grosz)", example = "3200")
        private Integer priceInCents;
        private Integer calories;
        @Schema(example = "MILD")
        private SpicinessLevel spiciness;

        @JsonProperty("ingredients")
        private List<RecipeItemResponseDTO> recipeItems;

        public DishDTO(Dish dish) {

                this.id = dish.getId();
                this.name = dish.getName();
                this.description = dish.getDescription();
                this.priceInCents = dish.getPriceInCents();
                this.calories = dish.getCalories();
                this.spiciness = dish.getSpiciness();

                this.recipeItems = dish.getRecipeItems().stream()
                                .map(RecipeItemResponseDTO::new)
                                .collect(Collectors.toList());

                add(linkTo(methodOn(DishController.class)
                                .get(dish.getId()))
                                .withSelfRel());

                add(linkTo(methodOn(DishController.class)
                                .getIngredients(dish.getId()))
                                .withRel("ingredients"));
        }
}