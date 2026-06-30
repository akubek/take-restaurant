package pl.polsl.take.restaurant.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.polsl.take.restaurant.model.enums.Allergen;
import pl.polsl.take.restaurant.model.enums.Unit;

@Entity
@Table(name = "ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Setter
    @NotBlank
    @Column(name = "ingredient_name")
    private String name;

    @Setter
    private Boolean isVegan;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private Unit unit;

    @ElementCollection(targetClass = Allergen.class)
    @CollectionTable(name = "ingredient_allergens", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "allergen")
    private Set<Allergen> allergens = new HashSet<>();

    public Ingredient(String name, Boolean isVegan, Unit unit, Set<Allergen> allergens) {
        this.name = name;
        this.isVegan = isVegan;
        this.unit = unit;
        if (allergens != null) {
            this.allergens = allergens;
        }
    }

    public void updateAllergens(Set<Allergen> newAllergens) {
        this.allergens.clear();
        if (newAllergens != null) {
            this.allergens.addAll(newAllergens);
        }
    }
}
