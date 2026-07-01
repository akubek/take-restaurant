package pl.polsl.take.restaurant.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateIngredientDTO;
import pl.polsl.take.restaurant.dto.IngredientDTO;
import pl.polsl.take.restaurant.dto.UpdateIngredientDTO;
import pl.polsl.take.restaurant.service.IngredientService;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
@Tag(name = "Ingredients", description = "Operations for managing ingredients")
public class IngredientController {

    private final IngredientService service;

    @GetMapping
    @Operation(summary = "Get all ingredients", description = "Returns all available ingredients")
    public List<IngredientDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ingredient by ID", description = "Returns one ingredient by identifier")
    public IngredientDTO get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create ingredient", description = "Creates a new ingredient")
    public IngredientDTO create(@Valid @RequestBody CreateIngredientDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ingredient", description = "Updates ingredient data")
    public IngredientDTO update(@PathVariable Long id, @Valid @RequestBody UpdateIngredientDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ingredient", description = "Deletes an ingredient if it is not used in any recipe")
    public IngredientDTO delete(@PathVariable Long id) {
        service.delete(id);
        return service.getById(id);
    }
}