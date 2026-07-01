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
// "Block customer deletion - allow data anonymization (soft delete)"
// "Customer value analysis - calculate total amount spent in the restaurant"
class CustomerIntegrationTest extends BaseIntegrationTest {

    private Long createCustomer(String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", firstName.toLowerCase() + "@test.com"
        );
        ResponseEntity<String> response = restTemplate.postForEntity("/customers", body, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    private Long createDishAndOrder(Long customerId, int priceInCents, int quantity) {
        // create dish
        Map<String, Object> dishBody = Map.of(
                "name", "Danie " + priceInCents,
                "description", "",
                "priceInCents", priceInCents,
                "calories", 400,
                "spiciness", "MILD"
        );
        ResponseEntity<String> dishResponse = restTemplate.postForEntity("/dishes", dishBody, String.class);
        Long dishId = ((Number) JsonPath.read(dishResponse.getBody(), "$.id")).longValue();

        // create and pay for order
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", quantity, "seatNumber", 1);
        Map<String, Object> orderBody = Map.of(
                "customerId", customerId,
                "tableNumber", 1,
                "items", List.of(item)
        );
        ResponseEntity<String> orderResponse = restTemplate.postForEntity("/orders", orderBody, String.class);
        Long orderId = ((Number) JsonPath.read(orderResponse.getBody(), "$.id")).longValue();

        // change status to DELIVERED and pay
        ResponseEntity<String> itemsResp = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Long itemId = ((Number) JsonPath.read(itemsResp.getBody(), "$[0].id")).longValue();
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                Map.of("status", "DELIVERED"), String.class
        );
        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        return orderId;
    }

    // -------------------------------------------------------------------------
    // SCENARIO 1: Anonymize customer - customer disappears from the list, history remains
    // -------------------------------------------------------------------------

    @Test
    void shouldAnonymizeCustomerButPreserveOrderHistory() {
        // Given - customer with an order
        Long customerId = createCustomer("Jan", "Kowalski");
        createDishAndOrder(customerId, 3200, 1);

        // verify that the customer is in the system
        ResponseEntity<String> beforeResponse = restTemplate.getForEntity("/customers/" + customerId, String.class);
        assertEquals(HttpStatus.OK, beforeResponse.getStatusCode());

        // When - anonymize customer
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/customers/" + customerId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Then 1 - customer is no longer available through the API
        ResponseEntity<String> afterResponse = restTemplate.getForEntity("/customers/" + customerId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, afterResponse.getStatusCode());

        // Then 2 - customer is not in the list of active customers
        ResponseEntity<String> listResponse = restTemplate.getForEntity("/customers", String.class);
        List<Integer> ids = JsonPath.read(listResponse.getBody(), "$[*].id");
        assertFalse(ids.stream().anyMatch(id -> id.longValue() == customerId));

        // Then 3 - customer's orders STILL exist in the database (history preserved)
        // Check through repository because customer API returns 404
        assertEquals(1, orderRepository.findByCustomerId(customerId).size());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 2: Analyze customer value - sum of paid orders
    // -------------------------------------------------------------------------

    @Test
    void shouldCalculateCorrectTotalSpendingForCustomer() {
        // Given - customer with two paid orders
        Long customerId = createCustomer("Anna", "Nowak");
        createDishAndOrder(customerId, 3200, 2); // 6400
        createDishAndOrder(customerId, 1500, 1); // 1500

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/customers/" + customerId + "/spending", String.class
        );

        // Then - total amount 7900
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7900L, Long.parseLong(response.getBody()));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 3: Cancelled orders do not count towards spending
    // -------------------------------------------------------------------------

    @Test
    void shouldNotCountCancelledOrdersInSpending() {
        // Given - one paid and one cancelled order
        Long customerId = createCustomer("Piotr", "Wiśniewski");
        createDishAndOrder(customerId, 3200, 1); // 3200 - paid

        // second order - cancelled
        Map<String, Object> dishBody = Map.of(
                "name", "Expensive pizza",
                "description", "",
                "priceInCents", 10000,
                "calories", 800,
                "spiciness", "HOT"
        );
        ResponseEntity<String> dishResp = restTemplate.postForEntity("/dishes", dishBody, String.class);
        Long dishId = ((Number) JsonPath.read(dishResp.getBody(), "$.id")).longValue();

        Map<String, Object> orderBody = Map.of(
                "customerId", customerId,
                "tableNumber", 2,
                "items", List.of(Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1))
        );
        ResponseEntity<String> orderResp = restTemplate.postForEntity("/orders", orderBody, String.class);
        Long cancelledOrderId = ((Number) JsonPath.read(orderResp.getBody(), "$.id")).longValue();
        restTemplate.delete("/orders/" + cancelledOrderId); // anuluj

        // When
        ResponseEntity<String> spendingResponse = restTemplate.getForEntity(
                "/customers/" + customerId + "/spending", String.class
        );

        // Then - only the paid order 3200 counts, cancelled (10000) does not
        assertEquals(3200L, Long.parseLong(spendingResponse.getBody()));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 4: Customer with zero spending
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnZeroSpendingForNewCustomer() {
        // Given - customer with no orders
        Long customerId = createCustomer("New", "Customer");

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/customers/" + customerId + "/spending", String.class
        );

        // Then - 0 instead of error
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, Long.parseLong(response.getBody()));
    }
}