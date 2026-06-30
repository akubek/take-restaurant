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

import pl.polsl.take.restaurant.dto.CreateIngredientDTO;
import pl.polsl.take.restaurant.dto.UpdateIngredientDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.GlobalExceptionHandler;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.model.enums.Unit;
import pl.polsl.take.restaurant.service.IngredientService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IngredientController.class)
@Import(GlobalExceptionHandler.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // GET /ingredients
    // -------------------------------------------------------------------------

    @Test
    void getAll_shouldReturn200WithList() throws Exception {
        when(ingredientService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /ingredients/{id}
    // -------------------------------------------------------------------------

    @Test
    void get_shouldReturn200WhenIngredientFound() throws Exception {
        mockMvc.perform(get("/ingredients/1"))
                .andExpect(status().isOk());
    }

    @Test
    void get_shouldReturn404WhenIngredientNotFound() throws Exception {
        when(ingredientService.getById(99L)).thenThrow(new NotFoundException("Ingredient not found"));

        mockMvc.perform(get("/ingredients/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /ingredients - walidacja @Valid
    // -------------------------------------------------------------------------

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("   "); // @NotBlank
        dto.setIsVegan(true);
        dto.setUnit(Unit.GRAM);

        mockMvc.perform(post("/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenIsVeganIsNull() throws Exception {
        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("Mąka");
        dto.setIsVegan(null); // @NotNull
        dto.setUnit(Unit.GRAM);

        mockMvc.perform(post("/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenUnitIsNull() throws Exception {
        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("Mąka");
        dto.setIsVegan(true);
        dto.setUnit(null); // @NotNull

        mockMvc.perform(post("/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn200WhenRequestIsValid() throws Exception {
        CreateIngredientDTO dto = new CreateIngredientDTO();
        dto.setName("Mąka");
        dto.setIsVegan(true);
        dto.setUnit(Unit.GRAM);

        mockMvc.perform(post("/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(ingredientService).create(any(CreateIngredientDTO.class));
    }

    // -------------------------------------------------------------------------
    // PUT /ingredients/{id}
    // -------------------------------------------------------------------------

    @Test
    void update_shouldReturn400WhenNameIsBlank() throws Exception {
        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName(""); // @NotBlank
        dto.setIsVegan(true);

        mockMvc.perform(put("/ingredients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn400WhenIsVeganIsNull() throws Exception {
        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName("Mąka");
        dto.setIsVegan(null); // @NotNull

        mockMvc.perform(put("/ingredients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn404WhenIngredientNotFound() throws Exception {
        when(ingredientService.update(eq(99L), any())).thenThrow(new NotFoundException("Ingredient not found"));

        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName("Mąka");
        dto.setIsVegan(true);

        mockMvc.perform(put("/ingredients/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200WhenValid() throws Exception {
        UpdateIngredientDTO dto = new UpdateIngredientDTO();
        dto.setName("Mąka Pszenna");
        dto.setIsVegan(true);

        mockMvc.perform(put("/ingredients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(ingredientService).update(eq(1L), any(UpdateIngredientDTO.class));
    }

    // -------------------------------------------------------------------------
    // DELETE /ingredients/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_shouldReturn200WhenIngredientExists() throws Exception {
        mockMvc.perform(delete("/ingredients/1"))
                .andExpect(status().isOk());

        verify(ingredientService).delete(1L);
    }

    @Test
    void delete_shouldReturn409WhenIngredientUsedInRecipe() throws Exception {
        // Given - składnik jest używany w przepisie → nie można usunąć
        doThrow(new ConflictException("Cannot delete ingredient because it is used in an existing recipe."))
                .when(ingredientService).delete(1L);

        mockMvc.perform(delete("/ingredients/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_shouldReturn404WhenIngredientNotFound() throws Exception {
        doThrow(new NotFoundException("Ingredient not found")).when(ingredientService).delete(99L);

        mockMvc.perform(delete("/ingredients/99"))
                .andExpect(status().isNotFound());
    }
}
