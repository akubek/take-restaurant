package pl.polsl.take.restaurant.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

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
public class DishController {

    private final DishService service;

    @GetMapping("/{id}")
    public DishDTO get(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/ingredients")
    public List<RecipeItemResponseDTO> getIngredients(@PathVariable Long id) {
        return service.getIngredients(id);
    }
    

    @GetMapping("/menu")
    public CollectionModel<DishDTO> getMenu() {
        List<DishDTO> dishes = service.getMenu();
        return CollectionModel.of(dishes, linkTo(methodOn(DishController.class).getMenu()).withSelfRel());
    }

    @GetMapping
    public CollectionModel<DishDTO> getAll() {
        List<DishDTO> dishes = service.getAllDishes();
        return CollectionModel.of(dishes, linkTo(methodOn(DishController.class).getAll()).withSelfRel());
    }


    @PostMapping
    public DishDTO create(@Valid @RequestBody CreateDishDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public DishDTO update(@PathVariable Long id, @Valid @RequestBody UpdateDishDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivateDish(@PathVariable Long id) {
        service.deactivateDish(id);
    }

    @PatchMapping("/{id}/reactivate")
    public void reactivateDish(@PathVariable Long id) {
        service.reactivateDish(id);
    }
}