package pl.polsl.take.restaurant.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end statistics tests for revenue aggregation and dish popularity
 * reporting.
 */
class StatsIntegrationTest extends BaseIntegrationTest {

    private void createAndPayOrder(Long customerId, Long dishId, int quantity) {
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", quantity, "seatNumber", 1);
        Map<String, Object> body = Map.of(
                "customerId", customerId,
                "tableNumber", 1,
                "items", List.of(item));
        ResponseEntity<String> resp = restTemplate.postForEntity("/orders", body, String.class);
        Long orderId = ((Number) JsonPath.read(resp.getBody(), "$.id")).longValue();

        ResponseEntity<String> itemsResp = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Integer itemId = ((Number) JsonPath.read(itemsResp.getBody(), "$[0].id")).intValue();
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                Map.of("status", "DELIVERED"), String.class);
        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);
    }

    private Long createCustomer() {
        Map<String, Object> body = Map.of("firstName", "Test", "lastName", "Client");
        ResponseEntity<String> resp = restTemplate.postForEntity("/customers", body, String.class);
        return ((Number) JsonPath.read(resp.getBody(), "$.id")).longValue();
    }

    private Long createDish(String name, int price) {
        Map<String, Object> body = Map.of(
                "name", name, "description", "", "priceInCents", price,
                "calories", 400, "spiciness", "MILD");
        ResponseEntity<String> resp = restTemplate.postForEntity("/dishes", body, String.class);
        return ((Number) JsonPath.read(resp.getBody(), "$.id")).longValue();
    }

    @Test
    void todayRevenue_shouldSumOnlyPaidOrdersFromToday() {
        // Given: two paid orders and one cancelled order from today
        Long customerId = createCustomer();
        Long dishId = createDish("Pizza", 3200);

        createAndPayOrder(customerId, dishId, 1);
        createAndPayOrder(customerId, dishId, 2);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 5, "seatNumber", 1);
        ResponseEntity<String> cancelResp = restTemplate.postForEntity("/orders",
                Map.of("customerId", customerId, "tableNumber", 2, "items", List.of(item)), String.class);
        Long cancelledId = ((Number) JsonPath.read(cancelResp.getBody(), "$.id")).longValue();
        restTemplate.delete("/orders/" + cancelledId);

        // When: today's revenue is requested
        ResponseEntity<String> response = restTemplate.getForEntity("/stats/revenue/today", String.class);

        // Then: only paid orders are counted
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(9600L, Long.parseLong(response.getBody()));
    }

    @Test
    void todayRevenue_shouldReturnZeroWhenNoOrders() {
        // When: revenue is requested for empty dataset
        ResponseEntity<String> response = restTemplate.getForEntity("/stats/revenue/today", String.class);

        // Then: zero revenue is returned
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, Long.parseLong(response.getBody()));
    }

    @Test
    void weekAndMonthRevenue_shouldBeAtLeastTodayRevenue() {
        // Given: one paid order in current time window
        Long customerId = createCustomer();
        Long dishId = createDish("Burger", 2500);
        createAndPayOrder(customerId, dishId, 2);

        // When: today/week/month revenues are requested
        long todayRevenue = Long.parseLong(
                restTemplate.getForEntity("/stats/revenue/today", String.class).getBody());
        long weekRevenue = Long.parseLong(
                restTemplate.getForEntity("/stats/revenue/week", String.class).getBody());
        long monthRevenue = Long.parseLong(
                restTemplate.getForEntity("/stats/revenue/month", String.class).getBody());

        // Then: wider windows are not smaller than narrower windows
        assertEquals(5000L, todayRevenue);
        assertTrue(weekRevenue >= todayRevenue, "Weekly revenue should be >= today's revenue");
        assertTrue(monthRevenue >= weekRevenue, "Monthly revenue should be >= weekly revenue");
    }

    @Test
    void dishPopularity_shouldCountCorrectlyAcrossMultipleOrders() {
        // Given: multiple paid orders with different dish quantities
        Long customerId = createCustomer();
        Long pizzaId = createDish("Pizza Margherita", 3200);
        Long burgerId = createDish("Burger Klasyczny", 2500);

        createAndPayOrder(customerId, pizzaId, 2);
        createAndPayOrder(customerId, pizzaId, 1);
        createAndPayOrder(customerId, burgerId, 1);

        String from = "2020-01-01T00:00:00";
        String to = "2030-12-31T23:59:59";

        // When: popularity report is requested for wide range
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stats/popularity?from=" + from + "&to=" + to, String.class);

        // Then: totals and names match expected aggregation
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<Map<String, Object>> results = JsonPath.read(response.getBody(), "$");
        assertEquals(2, results.size(), "There should be 2 dishes in the report");

        Map<String, Object> pizzaStats = results.stream()
                .filter(r -> ((Number) r.get("dishId")).longValue() == pizzaId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pizza not found in the report"));

        Map<String, Object> burgerStats = results.stream()
                .filter(r -> ((Number) r.get("dishId")).longValue() == burgerId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Burger not found in the report"));

        assertEquals(3L, ((Number) pizzaStats.get("totalSold")).longValue());
        assertEquals(1L, ((Number) burgerStats.get("totalSold")).longValue());
        assertEquals("Pizza Margherita", pizzaStats.get("dishName"));
        assertEquals("Burger Klasyczny", burgerStats.get("dishName"));
    }

    @Test
    void dishPopularity_shouldReturnEmptyListWhenNoOrders() {
        // Given: no orders in selected range
        String from = "2026-06-01T00:00:00";
        String to = "2026-06-30T23:59:59";

        // When: popularity is requested
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stats/popularity?from=" + from + "&to=" + to, String.class);

        // Then: empty list is returned
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> results = JsonPath.read(response.getBody(), "$");
        assertTrue(results.isEmpty());
    }

    @Test
    void dishPopularity_shouldNotCountCancelledItems() {
        // Given: order with one cancelled item and one delivered item
        Long customerId = createCustomer();
        Long pizzaId = createDish("Pizza", 3200);
        Long burgerId = createDish("Burger", 2500);

        List<Map<String, Object>> items = List.of(
                Map.of("dishId", pizzaId, "quantity", 1, "seatNumber", 1),
                Map.of("dishId", burgerId, "quantity", 1, "seatNumber", 2));
        ResponseEntity<String> orderResp = restTemplate.postForEntity("/orders",
                Map.of("customerId", customerId, "tableNumber", 1, "items", items), String.class);
        Long orderId = ((Number) JsonPath.read(orderResp.getBody(), "$.id")).longValue();

        ResponseEntity<String> itemsResp = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        List<Number> itemIds = JsonPath.read(itemsResp.getBody(), "$[*].id");
        List<Number> dishIds = JsonPath.read(itemsResp.getBody(), "$[*].dishId");

        for (int i = 0; i < dishIds.size(); i++) {
            if (dishIds.get(i).longValue() == burgerId) {
                restTemplate.delete("/orders/" + orderId + "/items/" + itemIds.get(i));
                break;
            }
        }

        for (int i = 0; i < dishIds.size(); i++) {
            if (dishIds.get(i).longValue() == pizzaId) {
                restTemplate.patchForObject(
                        "/orders/" + orderId + "/items/" + itemIds.get(i) + "/status",
                        Map.of("status", "DELIVERED"), String.class);
                break;
            }
        }
        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        // When: popularity report is requested
        ResponseEntity<String> statsResp = restTemplate.getForEntity(
                "/stats/popularity?from=2020-01-01T00:00:00&to=2030-12-31T23:59:59", String.class);

        // Then: cancelled dish is excluded, delivered dish is included
        List<Map<String, Object>> results = JsonPath.read(statsResp.getBody(), "$");
        boolean burgerInStats = results.stream()
                .anyMatch(r -> ((Number) r.get("dishId")).longValue() == burgerId);
        assertFalse(burgerInStats, "Cancelled burger should not be in the popularity report");

        boolean pizzaInStats = results.stream()
                .anyMatch(r -> ((Number) r.get("dishId")).longValue() == pizzaId);
        assertTrue(pizzaInStats, "Pizza should be in the popularity report");
    }
}