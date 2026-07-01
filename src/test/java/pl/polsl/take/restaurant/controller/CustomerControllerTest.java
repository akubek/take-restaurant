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

import pl.polsl.take.restaurant.dto.CreateCustomerDTO;
import pl.polsl.take.restaurant.dto.UpdateCustomerDTO;
import pl.polsl.take.restaurant.exception.GlobalExceptionHandler;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.service.CustomerService;
import pl.polsl.take.restaurant.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // GET /customers/{id}
    // -------------------------------------------------------------------------

    @Test
    void get_shouldReturn200WhenCustomerFound() throws Exception {
        // Given - serwis nie rzuca wyjątku (domyślne zachowanie mocka → null)
        mockMvc.perform(get("/customers/1"))
                .andExpect(status().isOk());
    }

    @Test
    void get_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.getById(99L)).thenThrow(new NotFoundException("Customer 99 not found"));

        mockMvc.perform(get("/customers/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /customers
    // -------------------------------------------------------------------------

    @Test
    void getAll_shouldReturn200WithEmptyList() throws Exception {
        when(customerService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // POST /customers - walidacja @Valid
    // -------------------------------------------------------------------------

    @Test
    void create_shouldReturn400WhenFirstNameIsBlank() throws Exception {
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setFirstName("   "); // @NotBlank
        dto.setLastName("Kowalski");

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenLastNameIsBlank() throws Exception {
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setFirstName("Jan");
        dto.setLastName(""); // @NotBlank

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenEmailIsInvalid() throws Exception {
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");
        dto.setEmail("not-an-email"); // @Email

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn200WhenRequestIsValid() throws Exception {
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");
        dto.setEmail("jan@test.com");

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(customerService).create(any(CreateCustomerDTO.class));
    }

    // -------------------------------------------------------------------------
    // PUT /customers/{id}
    // -------------------------------------------------------------------------

    @Test
    void update_shouldReturn400WhenFirstNameIsBlank() throws Exception {
        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setFirstName(""); // @NotBlank
        dto.setLastName("Kowalski");

        mockMvc.perform(put("/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn400WhenLastNameIsBlank() throws Exception {
        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setFirstName("Jan");
        dto.setLastName("  "); // @NotBlank

        mockMvc.perform(put("/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.update(eq(99L), any())).thenThrow(new NotFoundException("Customer not found"));

        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");

        mockMvc.perform(put("/customers/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /customers/{id} (anonymize)
    // -------------------------------------------------------------------------

    @Test
    void anonymize_shouldReturn204WhenCustomerExists() throws Exception {
        mockMvc.perform(delete("/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).anonymize(1L);
    }

    @Test
    void anonymize_shouldReturn404WhenCustomerNotFound() throws Exception {
        doThrow(new NotFoundException("Customer not found")).when(customerService).anonymize(99L);

        mockMvc.perform(delete("/customers/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /customers/{id}/spending
    // -------------------------------------------------------------------------

    @Test
    void getSpending_shouldReturn200() throws Exception {
        when(customerService.getTotalSpending(1L)).thenReturn(5000L);

        mockMvc.perform(get("/customers/1/spending"))
                .andExpect(status().isOk())
                .andExpect(content().string("5000"));
    }

    @Test
    void getSpending_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.getTotalSpending(99L)).thenThrow(new NotFoundException("Customer not found"));

        mockMvc.perform(get("/customers/99/spending"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /customers/{id}/orders
    // -------------------------------------------------------------------------

    @Test
    void orders_shouldReturn200() throws Exception {
        when(orderService.getByCustomerId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/customers/1/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void orders_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(orderService.getByCustomerId(99L)).thenThrow(new NotFoundException("Customer not found"));

        mockMvc.perform(get("/customers/99/orders"))
                .andExpect(status().isNotFound());
    }
}
