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
 * End-to-end tests for customer use-cases: anonymization and spending
 * calculations.
 */
class CustomerIntegrationTest extends BaseIntegrationTest {

    private Long createCustomer(String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", firstName.toLowerCase() + "@test.com");
        ResponseEntity<String> response = restTemplate.postForEntity("/customers", body, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    private Long createDishAndOrder(Long customerId, int priceInCents, int quantity) {

        Map<String, Object> dishBody = Map.of(
                "name", "Danie " + priceInCents,
                "description", "",
                "priceInCents", priceInCents,
                "calories", 400,
                "spiciness", "MILD");
        ResponseEntity<String> dishResponse = restTemplate.postForEntity("/dishes", dishBody, String.class);
        Long dishId = ((Number) JsonPath.read(dishResponse.getBody(), "$.id")).longValue();

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", quantity, "seatNumber", 1);
        Map<String, Object> orderBody = Map.of(
                "customerId", customerId,
                "tableNumber", 1,
                "items", List.of(item));
        ResponseEntity<String> orderResponse = restTemplate.postForEntity("/orders", orderBody, String.class);
        Long orderId = ((Number) JsonPath.read(orderResponse.getBody(), "$.id")).longValue();

        ResponseEntity<String> itemsResp = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Long itemId = ((Number) JsonPath.read(itemsResp.getBody(), "$[0].id")).longValue();
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                Map.of("status", "DELIVERED"), String.class);
        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        return orderId;
    }

    @Test
    void shouldAnonymizeCustomerButPreserveOrderHistory() {
        // Given: customer with paid order history
        Long customerId = createCustomer("Jan", "Kowalski");
        createDishAndOrder(customerId, 3200, 1);

        ResponseEntity<String> beforeResponse = restTemplate.getForEntity("/customers/" + customerId, String.class);
        assertEquals(HttpStatus.OK, beforeResponse.getStatusCode());

        // When: customer is anonymized (soft-deleted)
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/customers/" + customerId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Then: customer is hidden from API but related order remains in DB
        ResponseEntity<String> afterResponse = restTemplate.getForEntity("/customers/" + customerId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, afterResponse.getStatusCode());

        ResponseEntity<String> listResponse = restTemplate.getForEntity("/customers", String.class);
        List<Integer> ids = JsonPath.read(listResponse.getBody(), "$[*].id");
        assertFalse(ids.stream().anyMatch(id -> id.longValue() == customerId));

        assertEquals(1, orderRepository.findByCustomerId(customerId).size());
    }

    @Test
    void shouldCalculateCorrectTotalSpendingForCustomer() {
        // Given: customer with multiple paid orders
        Long customerId = createCustomer("Anna", "Nowak");
        createDishAndOrder(customerId, 3200, 2);
        createDishAndOrder(customerId, 1500, 1);

        // When: total spending is requested
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/customers/" + customerId + "/spending", String.class);

        // Then: spending equals sum of paid order items
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7900L, Long.parseLong(response.getBody()));
    }

    @Test
    void shouldNotCountCancelledOrdersInSpending() {
        // Given: one paid order and one cancelled order for same customer
        Long customerId = createCustomer("Piotr", "Wiśniewski");
        createDishAndOrder(customerId, 3200, 1);

        Map<String, Object> dishBody = Map.of(
                "name", "Expensive pizza",
                "description", "",
                "priceInCents", 10000,
                "calories", 800,
                "spiciness", "HOT");
        ResponseEntity<String> dishResp = restTemplate.postForEntity("/dishes", dishBody, String.class);
        Long dishId = ((Number) JsonPath.read(dishResp.getBody(), "$.id")).longValue();

        Map<String, Object> orderBody = Map.of(
                "customerId", customerId,
                "tableNumber", 2,
                "items", List.of(Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1)));
        ResponseEntity<String> orderResp = restTemplate.postForEntity("/orders", orderBody, String.class);
        Long cancelledOrderId = ((Number) JsonPath.read(orderResp.getBody(), "$.id")).longValue();
        restTemplate.delete("/orders/" + cancelledOrderId);

        // When: spending is requested
        ResponseEntity<String> spendingResponse = restTemplate.getForEntity(
                "/customers/" + customerId + "/spending", String.class);

        // Then: cancelled order is excluded from total
        assertEquals(3200L, Long.parseLong(spendingResponse.getBody()));
    }

    @Test
    void shouldReturnZeroSpendingForNewCustomer() {
        // Given: new customer without any orders
        Long customerId = createCustomer("New", "Customer");

        // When: spending is requested
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/customers/" + customerId + "/spending", String.class);

        // Then: zero is returned
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, Long.parseLong(response.getBody()));
    }
}