package pl.polsl.take.restaurant.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;

@Entity
@Table(name = "dish")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank
    @Column(name = "dish_name")
    private String name;

    @Setter
    private String description;

    @Setter
    @NotNull
    @Column(nullable = false)
    private Integer priceInCents; // or "grosze"
    
    @NotNull
    @Column(nullable = false, updatable = false)
    private Integer calories; // not calculated from ingredients because it might be slightly different (f.e fried food using oil - oil not counted as and ingredient, but kcal is higher)
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(updatable = false, nullable = false)
    private SpicinessLevel spiciness;
    
    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeItem> recipeItems = new ArrayList<>();

    @Setter
    @NotNull
    @Column(nullable = false)
    private Boolean isActive = true; // is the dish an active menu item

    public Dish(String name, String description, Integer priceInCents, Integer calories, SpicinessLevel spiciness) {
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.calories = calories;
        this.spiciness = spiciness;
    }
}


