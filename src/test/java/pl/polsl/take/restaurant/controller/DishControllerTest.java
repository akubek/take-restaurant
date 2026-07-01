package pl.polsl.take.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.polsl.take.restaurant.dto.CreateDishDTO;
import pl.polsl.take.restaurant.dto.UpdateDishDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.GlobalExceptionHandler;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.enums.SpicinessLevel;
import pl.polsl.take.restaurant.service.DishService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DishController.class)
@Import(GlobalExceptionHandler.class)
/**
 * Controller-layer tests for dish endpoints, including validation and conflict/not-found handling.
 */
class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DishService dishService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void get_shouldReturn200WhenDishFound() throws Exception {
        mockMvc.perform(get("/dishes/1"))
                .andExpect(status().isOk());
    }

    @Test
    void get_shouldReturn404WhenDishNotFound() throws Exception {
        when(dishService.getById(99L)).thenThrow(new NotFoundException("Dish not found"));

        mockMvc.perform(get("/dishes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIngredients_shouldReturn200WhenDishFound() throws Exception {
        when(dishService.getIngredients(1L)).thenReturn(List.of());

        mockMvc.perform(get("/dishes/1/ingredients"))
                .andExpect(status().isOk());
    }

    @Test
    void getIngredients_shouldReturn404WhenDishNotFound() throws Exception {
        when(dishService.getIngredients(99L)).thenThrow(new NotFoundException("Dish not found"));

        mockMvc.perform(get("/dishes/99/ingredients"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMenu_shouldReturn200() throws Exception {
        when(dishService.getMenu()).thenReturn(List.of());

        mockMvc.perform(get("/dishes/menu"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        when(dishService.getAllDishes()).thenReturn(List.of());

        mockMvc.perform(get("/dishes"))
                .andExpect(status().isOk());
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("   ");
        dto.setPriceInCents(3200);
        dto.setCalories(1000);
        dto.setSpiciness(SpicinessLevel.MILD);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenPriceIsNull() throws Exception {
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(null);
        dto.setCalories(1000);
        dto.setSpiciness(SpicinessLevel.MILD);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenPriceIsNegative() throws Exception {
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(-100);
        dto.setCalories(1000);
        dto.setSpiciness(SpicinessLevel.MILD);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenCaloriesIsNull() throws Exception {
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(3200);
        dto.setCalories(null);
        dto.setSpiciness(SpicinessLevel.MILD);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenSpicinessIsNull() throws Exception {
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(3200);
        dto.setCalories(1000);
        dto.setSpiciness(null);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn200WhenRequestIsValid() throws Exception {
        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza Margherita");
        dto.setPriceInCents(3200);
        dto.setCalories(1000);
        dto.setSpiciness(SpicinessLevel.MILD);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(dishService).create(any(CreateDishDTO.class));
    }

    @Test
    void create_shouldReturn409WhenConflict() throws Exception {

        when(dishService.create(any())).thenThrow(new ConflictException("Invalid recipe item configuration."));

        CreateDishDTO dto = new CreateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(3200);
        dto.setCalories(1000);
        dto.setSpiciness(SpicinessLevel.MILD);

        mockMvc.perform(post("/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void update_shouldReturn400WhenNameIsBlank() throws Exception {
        UpdateDishDTO dto = new UpdateDishDTO();
        dto.setName("");
        dto.setPriceInCents(3200);

        mockMvc.perform(put("/dishes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn400WhenPriceIsNull() throws Exception {
        UpdateDishDTO dto = new UpdateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(null);

        mockMvc.perform(put("/dishes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn404WhenDishNotFound() throws Exception {
        when(dishService.update(eq(99L), any())).thenThrow(new NotFoundException("Dish not found"));

        UpdateDishDTO dto = new UpdateDishDTO();
        dto.setName("Pizza");
        dto.setPriceInCents(3200);

        mockMvc.perform(put("/dishes/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn200WhenDishExists() throws Exception {
        mockMvc.perform(delete("/dishes/1"))
                .andExpect(status().isOk());

        verify(dishService).delete(1L);
    }

    @Test
    void delete_shouldReturn404WhenDishNotFound() throws Exception {
        doThrow(new NotFoundException("Dish not found")).when(dishService).delete(99L);

        mockMvc.perform(delete("/dishes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivateDish_shouldReturn200WhenDishExists() throws Exception {
        mockMvc.perform(patch("/dishes/1/deactivate"))
                .andExpect(status().isOk());

        verify(dishService).deactivateDish(1L);
    }

    @Test
    void deactivateDish_shouldReturn404WhenDishNotFound() throws Exception {
        doThrow(new NotFoundException("Dish not found")).when(dishService).deactivateDish(99L);

        mockMvc.perform(patch("/dishes/99/deactivate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reactivateDish_shouldReturn200WhenDishExists() throws Exception {
        mockMvc.perform(patch("/dishes/1/reactivate"))
                .andExpect(status().isOk());

        verify(dishService).reactivateDish(1L);
    }

    @Test
    void reactivateDish_shouldReturn404WhenDishNotFound() throws Exception {
        doThrow(new NotFoundException("Dish not found")).when(dishService).reactivateDish(99L);

        mockMvc.perform(patch("/dishes/99/reactivate"))
                .andExpect(status().isNotFound());
    }
}
