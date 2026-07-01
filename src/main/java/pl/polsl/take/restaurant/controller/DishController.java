package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateDishDTO;
import pl.polsl.take.restaurant.dto.DishDTO;
import pl.polsl.take.restaurant.dto.RecipeItemResponseDTO;
import pl.polsl.take.restaurant.dto.UpdateDishDTO;
import pl.polsl.take.restaurant.service.DishService;

@RestController
@RequestMapping("/dishes")
@RequiredArgsConstructor
@Tag(name = "Dishes", description = "Operations for managing menu dishes")
public class DishController {

    private final DishService service;

    @GetMapping("/{id}")
    @Operation(summary = "Get dish by ID", description = "Returns one active dish by identifier")
    public DishDTO get(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/ingredients")
    @Operation(summary = "Get dish ingredients", description = "Returns recipe ingredients for the selected dish")
    public List<RecipeItemResponseDTO> getIngredients(@PathVariable Long id) {
        return service.getIngredients(id);
    }

    @GetMapping("/menu")
    @Operation(summary = "Get active menu", description = "Returns all currently active menu dishes")
    public CollectionModel<DishDTO> getMenu() {
        List<DishDTO> dishes = service.getMenu();
        return CollectionModel.of(dishes, linkTo(methodOn(DishController.class).getMenu()).withSelfRel());
    }

    @GetMapping
    @Operation(summary = "Get all dishes", description = "Returns all dishes, including inactive ones")
    public CollectionModel<DishDTO> getAll() {
        List<DishDTO> dishes = service.getAllDishes();
        return CollectionModel.of(dishes, linkTo(methodOn(DishController.class).getAll()).withSelfRel());
    }

    @PostMapping
    @Operation(summary = "Create dish", description = "Creates a new dish with optional recipe items")
    public DishDTO create(@Valid @RequestBody CreateDishDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dish", description = "Updates basic dish fields for the selected dish")
    public DishDTO update(@PathVariable Long id, @Valid @RequestBody UpdateDishDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dish", description = "Deletes a dish or soft-deactivates it if it has order history")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate dish", description = "Marks a dish as inactive in the menu")
    public DishDTO deactivateDish(@PathVariable Long id) {
        service.deactivateDish(id);
        return service.getById(id);
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate dish", description = "Marks a previously inactive dish as active")
    public DishDTO reactivateDish(@PathVariable Long id) {
        service.reactivateDish(id);
        return service.getById(id);
    }
}