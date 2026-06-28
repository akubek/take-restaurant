package pl.polsl.take.restaurant.controller;

import java.util.List;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.DishService;
import pl.polsl.take.restaurant.dto.CreateDishDTO;
import pl.polsl.take.restaurant.dto.DishDTO;
import pl.polsl.take.restaurant.model.Ingredient;
@RestController
@RequestMapping("/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService service;

    @PostMapping
    public DishDTO create(@RequestBody CreateDishDTO dto) {
        return new DishDTO(service.createDish(dto));
    }

    @GetMapping("/{id}")
    public DishDTO get(@PathVariable Long id) {
        return new DishDTO(service.getById(id));
    }

    @GetMapping
    public CollectionModel<DishDTO> getAll() {

        List<DishDTO> orders = service.getAll()
                .stream()
                .map(DishDTO::new)
                .toList();

        return CollectionModel.of(
                orders,
                linkTo(methodOn(DishController.class)
                        .getAll())
                        .withSelfRel()
        );
    }
   

    @GetMapping("/{id}/ingredients")
    public List<String> ingredients(@PathVariable Long id) {
        return service.getById(id)
                .getRecipeItems()
                .stream()
                .map(ri -> ri.getIngredient().getName())
                .toList();
    }
}