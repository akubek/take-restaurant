package pl.polsl.take.restaurant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recipe_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(updatable = false, nullable = false)
    private Double amount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id",updatable = false, nullable = false)
    private Ingredient ingredient;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", updatable = false, nullable = false)
    private Dish dish;

    public RecipeItem(Dish dish, Ingredient ingredient, Double amount) {
        this.dish = dish;
        this.ingredient = ingredient;
        this.amount = amount;
    }
}
