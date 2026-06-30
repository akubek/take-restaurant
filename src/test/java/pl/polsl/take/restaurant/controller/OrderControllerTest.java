package pl.polsl.take.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.polsl.take.restaurant.dto.CreateOrderDTO;
import pl.polsl.take.restaurant.dto.CreateOrderItemDTO;
import pl.polsl.take.restaurant.exception.ConflictException;
import pl.polsl.take.restaurant.exception.GlobalExceptionHandler;
import pl.polsl.take.restaurant.exception.NotFoundException;
import pl.polsl.take.restaurant.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest ładuje TYLKO warstwę HTTP: kontroler + filtry + walidację
// Nie startuje bazy danych ani serwisów - te są mockowane przez @MockBean
// Testujemy: routing, walidację requestów, kody HTTP, obsługę wyjątków
@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // wymagane do serializacji LocalDateTime w CreateOrderDTO
        objectMapper.registerModule(new JavaTimeModule());
    }

    // -------------------------------------------------------------------------
    // POST /orders - walidacja wejścia (@Valid na @RequestBody)
    // -------------------------------------------------------------------------

    @Test
    void createOrder_shouldReturn400WhenBodyIsEmpty() throws Exception {
        // Given - pusty JSON nie spełnia @NotEmpty na items
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenItemsListIsEmpty() throws Exception {
        // Given - items: [] narusza @NotEmpty
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setTableNumber(1);
        dto.setItems(List.of());

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenItemHasNullDishId() throws Exception {
        // Given - item bez dishId narusza @NotNull
        CreateOrderItemDTO item = new CreateOrderItemDTO();
        item.setDishId(null);  // @NotNull
        item.setQuantity(1);

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setTableNumber(1);
        dto.setItems(List.of(item));

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenQuantityIsZero() throws Exception {
        // Given - quantity=0 narusza @Min(1)
        CreateOrderItemDTO item = new CreateOrderItemDTO();
        item.setDishId(1L);
        item.setQuantity(0);

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setTableNumber(1);
        dto.setItems(List.of(item));

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /orders/{id} - obsługa NotFoundException → 404
    // -------------------------------------------------------------------------

    @Test
    void getOrder_shouldReturn404WhenOrderDoesNotExist() throws Exception {
        // Given
        when(orderService.getById(99L)).thenThrow(new NotFoundException("Order 99 not found"));

        // When / Then
        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /orders/{id}/pay - obsługa ConflictException → 409
    // -------------------------------------------------------------------------

    @Test
    void payOrder_shouldReturn409WhenOrderAlreadyPaid() throws Exception {
        // Given - serwis rzuca ConflictException (logika biznesowa już przetestowana w serwisie)
        when(orderService.payOrder(1L))
                .thenThrow(new ConflictException("The order was already paid."));

        // When / Then
        mockMvc.perform(patch("/orders/1/pay"))
                .andExpect(status().isConflict());
    }

    @Test
    void payOrder_shouldReturn409WhenOrderHasPendingItems() throws Exception {
        when(orderService.payOrder(1L))
                .thenThrow(new ConflictException("Order contains items that have not been delivered."));

        mockMvc.perform(patch("/orders/1/pay"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // DELETE /orders/{id} - anulowanie zamówienia
    // -------------------------------------------------------------------------

    @Test
    void cancelOrder_shouldReturn409WhenOrderAlreadyPaid() throws Exception {
        doThrow(new ConflictException("Cannot cancel a paid order."))
                .when(orderService).cancelOrder(1L);

        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_shouldReturn404WhenOrderDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Order not found."))
                .when(orderService).cancelOrder(99L);

        mockMvc.perform(delete("/orders/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /orders/{orderId}/items/{itemId}
    // -------------------------------------------------------------------------

    @Test
    void cancelItem_shouldReturn409WhenOrderIsPaid() throws Exception {
        doThrow(new ConflictException("Cannot modify items in a paid order."))
                .when(orderService).cancelOrderItem(1L, 10L);

        mockMvc.perform(delete("/orders/1/items/10"))
                .andExpect(status().isConflict());
    }
}