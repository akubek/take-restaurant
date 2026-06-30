package pl.polsl.take.restaurant.controller;

import java.util.List;

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
public class IngredientController {

    private final IngredientService service;

    @GetMapping
    public List<IngredientDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public IngredientDTO get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public IngredientDTO create(@Valid @RequestBody CreateIngredientDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public IngredientDTO update(@PathVariable Long id, @Valid @RequestBody UpdateIngredientDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}