package pl.polsl.take.restaurant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.polsl.take.restaurant.exception.GlobalExceptionHandler;
import pl.polsl.take.restaurant.service.StatsService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatsController.class)
@Import(GlobalExceptionHandler.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    // -------------------------------------------------------------------------
    // GET /stats/revenue/today
    // -------------------------------------------------------------------------

    @Test
    void todayRevenue_shouldReturn200WithValue() throws Exception {
        when(statsService.getTodayRevenue()).thenReturn(5000L);

        mockMvc.perform(get("/stats/revenue/today"))
                .andExpect(status().isOk())
                .andExpect(content().string("5000"));
    }

    @Test
    void todayRevenue_shouldReturn200WithZeroWhenNoOrders() throws Exception {
        when(statsService.getTodayRevenue()).thenReturn(0L);

        mockMvc.perform(get("/stats/revenue/today"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // -------------------------------------------------------------------------
    // GET /stats/revenue/week
    // -------------------------------------------------------------------------

    @Test
    void weekRevenue_shouldReturn200WithValue() throws Exception {
        when(statsService.getWeekRevenue()).thenReturn(42000L);

        mockMvc.perform(get("/stats/revenue/week"))
                .andExpect(status().isOk())
                .andExpect(content().string("42000"));
    }

    @Test
    void weekRevenue_shouldReturn200WithZeroWhenNoOrders() throws Exception {
        when(statsService.getWeekRevenue()).thenReturn(0L);

        mockMvc.perform(get("/stats/revenue/week"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // -------------------------------------------------------------------------
    // GET /stats/revenue/month
    // -------------------------------------------------------------------------

    @Test
    void monthRevenue_shouldReturn200WithValue() throws Exception {
        when(statsService.getMonthRevenue()).thenReturn(150000L);

        mockMvc.perform(get("/stats/revenue/month"))
                .andExpect(status().isOk())
                .andExpect(content().string("150000"));
    }

    // -------------------------------------------------------------------------
    // GET /stats/revenue?from=...&to=...
    // -------------------------------------------------------------------------

    @Test
    void revenueBetween_shouldReturn200WithValidParams() throws Exception {
        when(statsService.getRevenueBetween(any(), any())).thenReturn(15000L);

        mockMvc.perform(get("/stats/revenue")
                .param("from", "2026-06-01T00:00:00")
                .param("to",   "2026-06-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(content().string("15000"));

        verify(statsService).getRevenueBetween(any(), any());
    }

    @Test
    void revenueBetween_shouldReturn200WithZeroWhenNoRevenue() throws Exception {
        when(statsService.getRevenueBetween(any(), any())).thenReturn(0L);

        mockMvc.perform(get("/stats/revenue")
                .param("from", "2026-01-01T00:00:00")
                .param("to",   "2026-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // -------------------------------------------------------------------------
    // GET /stats/popularity?from=...&to=...
    // -------------------------------------------------------------------------

    @Test
    void dishPopularity_shouldReturn200WithEmptyList() throws Exception {
        when(statsService.getDishPopularity(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/stats/popularity")
                .param("from", "2026-06-01T00:00:00")
                .param("to",   "2026-06-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void dishPopularity_shouldReturn200WithResults() throws Exception {
        var dto = new pl.polsl.take.restaurant.dto.DishPopularityDTO(1L, "Pizza", 15L);
        when(statsService.getDishPopularity(any(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/stats/popularity")
                .param("from", "2026-06-01T00:00:00")
                .param("to",   "2026-06-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dishId").value(1))
                .andExpect(jsonPath("$[0].dishName").value("Pizza"))
                .andExpect(jsonPath("$[0].totalSold").value(15));
    }
}
