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

// Test use case scenarios:
// "Financial report - revenue statistics"
// "Dish popularity report - sales statistics"
// This is the only layer of tests where we verify the correctness of SQL aggregation on real data.
class StatsIntegrationTest extends BaseIntegrationTest {

    // helper: creates and pays for an order, returns orderId
    private void createAndPayOrder(Long customerId, Long dishId, int quantity) {
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", quantity, "seatNumber", 1);
        Map<String, Object> body = Map.of(
                "customerId", customerId,
                "tableNumber", 1,
                "items", List.of(item)
        );
        ResponseEntity<String> resp = restTemplate.postForEntity("/orders", body, String.class);
        Long orderId = ((Number) JsonPath.read(resp.getBody(), "$.id")).longValue();

        ResponseEntity<String> itemsResp = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Integer itemId = ((Number) JsonPath.read(itemsResp.getBody(), "$[0].id")).intValue();
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                Map.of("status", "DELIVERED"), String.class
        );
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
                "calories", 400, "spiciness", "MILD"
        );
        ResponseEntity<String> resp = restTemplate.postForEntity("/dishes", body, String.class);
        return ((Number) JsonPath.read(resp.getBody(), "$.id")).longValue();
    }

    // -------------------------------------------------------------------------
    // SCENARIO 1: Today's revenue - correct sum of paid orders
    // -------------------------------------------------------------------------

    @Test
    void todayRevenue_shouldSumOnlyPaidOrdersFromToday() {
        // Given - 3 orders: 2 paid + 1 cancelled
        Long customerId = createCustomer();
        Long dishId = createDish("Pizza", 3200);

        createAndPayOrder(customerId, dishId, 1); // 3200
        createAndPayOrder(customerId, dishId, 2); // 6400

        // cancelled order - NOT included in the report
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 5, "seatNumber", 1);
        ResponseEntity<String> cancelResp = restTemplate.postForEntity("/orders",
                Map.of("customerId", customerId, "tableNumber", 2, "items", List.of(item)), String.class);
        Long cancelledId = ((Number) JsonPath.read(cancelResp.getBody(), "$.id")).longValue();
        restTemplate.delete("/orders/" + cancelledId);

        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/stats/revenue/today", String.class);

        // Then - only paid orders: 3200 + 6400 = 9600
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(9600L, Long.parseLong(response.getBody()));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 2: Zero revenue when no orders
    // -------------------------------------------------------------------------

    @Test
    void todayRevenue_shouldReturnZeroWhenNoOrders() {
        // Given - empty database (cleanDatabase in BeforeEach)

        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/stats/revenue/today", String.class);

        // Then - 0 instead of 500 error
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, Long.parseLong(response.getBody()));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 3: Weekly and monthly revenue - should be >= today's revenue
    // -------------------------------------------------------------------------

    @Test
    void weekAndMonthRevenue_shouldBeAtLeastTodayRevenue() {
        // Given - two paid orders today
        Long customerId = createCustomer();
        Long dishId = createDish("Burger", 2500);
        createAndPayOrder(customerId, dishId, 2); // 5000

        // When
        long todayRevenue = Long.parseLong(
                restTemplate.getForEntity("/stats/revenue/today", String.class).getBody());
        long weekRevenue = Long.parseLong(
                restTemplate.getForEntity("/stats/revenue/week", String.class).getBody());
        long monthRevenue = Long.parseLong(
                restTemplate.getForEntity("/stats/revenue/month", String.class).getBody());

        // Then - week and month include today, so they must be >= today's revenue
        assertEquals(5000L, todayRevenue);
        assertTrue(weekRevenue >= todayRevenue, "Weekly revenue should be >= today's revenue");
        assertTrue(monthRevenue >= weekRevenue, "Monthly revenue should be >= weekly revenue");
    }

    // -------------------------------------------------------------------------
    // SCENARIO 4: Dish popularity - correct counting of sold items
    // -------------------------------------------------------------------------

    @Test
    void dishPopularity_shouldCountCorrectlyAcrossMultipleOrders() {
        // Given
        Long customerId = createCustomer();
        Long pizzaId = createDish("Pizza Margherita", 3200);
        Long burgerId = createDish("Burger Klasyczny", 2500);

        // 2 orders of pizza (total 3 pieces) and 1 order of burger (1 piece)
        createAndPayOrder(customerId, pizzaId, 2);  // 2 pizzas
        createAndPayOrder(customerId, pizzaId, 1);  // 1 pizza
        createAndPayOrder(customerId, burgerId, 1); // 1 burger

        String from = "2020-01-01T00:00:00";
        String to   = "2030-12-31T23:59:59";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stats/popularity?from=" + from + "&to=" + to, String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<Map<String, Object>> results = JsonPath.read(response.getBody(), "$");
        assertEquals(2, results.size(), "There should be 2 dishes in the report");

        // find pizza and burger in the results (order may vary)
        Map<String, Object> pizzaStats = results.stream()
                .filter(r -> ((Number) r.get("dishId")).longValue() == pizzaId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pizza not found in the report"));

        Map<String, Object> burgerStats = results.stream()
                .filter(r -> ((Number) r.get("dishId")).longValue() == burgerId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Burger not found in the report"));

        assertEquals(3L, ((Number) pizzaStats.get("totalSold")).longValue());  // 2 + 1
        assertEquals(1L, ((Number) burgerStats.get("totalSold")).longValue()); // 1
        assertEquals("Pizza Margherita", pizzaStats.get("dishName"));
        assertEquals("Burger Klasyczny", burgerStats.get("dishName"));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 5: Dish popularity - empty list when no orders
    // -------------------------------------------------------------------------

    @Test
    void dishPopularity_shouldReturnEmptyListWhenNoOrders() {
        // Given - empty database

        String from = "2026-06-01T00:00:00";
        String to   = "2026-06-30T23:59:59";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stats/popularity?from=" + from + "&to=" + to, String.class
        );

        // Then - empty list, not a 500 error
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> results = JsonPath.read(response.getBody(), "$");
        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 6: Cancelled items should not count towards popularity
    // -------------------------------------------------------------------------

    @Test
    void dishPopularity_shouldNotCountCancelledItems() {
        // Given - order with one valid and one cancelled item
        Long customerId = createCustomer();
        Long pizzaId  = createDish("Pizza", 3200);
        Long burgerId = createDish("Burger", 2500);

        // create order with pizza and burger
        List<Map<String, Object>> items = List.of(
                Map.of("dishId", pizzaId,  "quantity", 1, "seatNumber", 1),
                Map.of("dishId", burgerId, "quantity", 1, "seatNumber", 2)
        );
        ResponseEntity<String> orderResp = restTemplate.postForEntity("/orders",
                Map.of("customerId", customerId, "tableNumber", 1, "items", items), String.class);
        Long orderId = ((Number) JsonPath.read(orderResp.getBody(), "$.id")).longValue();

        // cancel burger (leave pizza)
        ResponseEntity<String> itemsResp = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        List<Number> itemIds = JsonPath.read(itemsResp.getBody(), "$[*].id");
        List<Number> dishIds = JsonPath.read(itemsResp.getBody(), "$[*].dishId");

        // find the id of the burger item
        for (int i = 0; i < dishIds.size(); i++) {
            if (dishIds.get(i).longValue() == burgerId) {
                restTemplate.delete("/orders/" + orderId + "/items/" + itemIds.get(i));
                break;
            }
        }

        // deliver pizza and pay
        for (int i = 0; i < dishIds.size(); i++) {
            if (dishIds.get(i).longValue() == pizzaId) {
                restTemplate.patchForObject(
                        "/orders/" + orderId + "/items/" + itemIds.get(i) + "/status",
                        Map.of("status", "DELIVERED"), String.class
                );
                break;
            }
        }
        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        // When
        ResponseEntity<String> statsResp = restTemplate.getForEntity(
                "/stats/popularity?from=2020-01-01T00:00:00&to=2030-12-31T23:59:59", String.class
        );

        // Then - only pizza (burger was cancelled)
        List<Map<String, Object>> results = JsonPath.read(statsResp.getBody(), "$");
        boolean burgerInStats = results.stream()
                .anyMatch(r -> ((Number) r.get("dishId")).longValue() == burgerId);
        assertFalse(burgerInStats, "Cancelled burger should not be in the popularity report");

        boolean pizzaInStats = results.stream()
                .anyMatch(r -> ((Number) r.get("dishId")).longValue() == pizzaId);
        assertTrue(pizzaInStats, "Pizza should be in the popularity report");
    }
}