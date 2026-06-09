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
    private String name;

    @Setter
    private String description;

    @Setter
    private Integer priceInCents; // or "grosze"
    
    @Column(updatable = false)
    private Integer calories; // not calculated from ingredients because it might be slightly different (f.e fried food using oil - oil not counted as and ingredient, but kcal is higher)
    
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private SpicinessLevel spiciness;
    
    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeItem> recipeItems = new ArrayList<>();

    public Dish(String name, String description, Integer priceInCents, Integer calories, SpicinessLevel spiciness) {
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.calories = calories;
        this.spiciness = spiciness;
    }
}


