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

// Test lifecycle for order:
// Creation of customer → dishes → order → change of item statuses → payment
// This is the only test layer that verifies that all layers work correctly together.
class OrderLifecycleIntegrationTest extends BaseIntegrationTest {

    // -------------------------------------------------------------------------
    // HELPER: creating data via HTTP (not via repo - this is an integration test!)
    // -------------------------------------------------------------------------

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

    private Long createDish(String name, int priceInCents) {
        Map<String, Object> body = Map.of(
                "name", name,
                "description", "Test dish",
                "priceInCents", priceInCents,
                "calories", 500,
                "spiciness", "MILD"
        );
        ResponseEntity<String> response = restTemplate.postForEntity("/dishes", body, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    private Long createOrder(Long customerId, Long dishId, int quantity) {
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", quantity, "seatNumber", 1);
        Map<String, Object> body = Map.of(
                "customerId", customerId,
                "tableNumber", 5,
                "items", List.of(item)
        );
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    // -------------------------------------------------------------------------
    // SCENARIO 1: Full happy path - creation to payment
    // -------------------------------------------------------------------------

    @Test
    void fullOrderLifecycle_fromCreationToPayment() {
        // Given - create customer and dish via HTTP
        Long customerId = createCustomer("Jan", "Kowalski");
        Long dishId = createDish("Pizza Margherita", 3200);

        // When - create order with 2 pizzas
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 2, "seatNumber", 1);
        Map<String, Object> orderBody = Map.of(
                "customerId", customerId,
                "tableNumber", 5,
                "items", List.of(item)
        );
        ResponseEntity<String> createResponse = restTemplate.postForEntity("/orders", orderBody, String.class);

        // Then - order has been created
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        Long orderId = ((Number) JsonPath.read(createResponse.getBody(), "$.id")).longValue();
        assertEquals("OPEN", JsonPath.read(createResponse.getBody(), "$.status"));
        assertEquals(6400L, ((Number) JsonPath.read(createResponse.getBody(), "$.totalPriceCents")).longValue()); // 2 * 3200

        // When - fetch order items and change the status of the first one to DELIVERED
        ResponseEntity<String> itemsResponse = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        assertEquals(HttpStatus.OK, itemsResponse.getStatusCode());
        Long itemId = ((Number) JsonPath.read(itemsResponse.getBody(), "$[0].id")).longValue();

        Map<String, String> statusBody = Map.of("status", "DELIVERED");
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                statusBody, String.class
        );

        // When - pay the order
        ResponseEntity<String> payResponse = restTemplate.exchange(
                "/orders/" + orderId + "/pay",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class
        );

        // Then - order paid, amount correct
        assertEquals(HttpStatus.OK, payResponse.getStatusCode());
        assertEquals("PAID", JsonPath.read(payResponse.getBody(), "$.status"));
        assertEquals(6400L, ((Number) JsonPath.read(payResponse.getBody(), "$.totalPriceCents")).longValue());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 2: Cannot pay when item is not DELIVERED
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn409WhenPayingOrderWithPendingItems() {
        // Given - order with an item in NEW status (default)
        Long customerId = createCustomer("Anna", "Nowak");
        Long dishId = createDish("Burger", 2500);
        Long orderId = createOrder(customerId, dishId, 1);

        // When - attempt to pay without changing item status
        ResponseEntity<String> payResponse = restTemplate.exchange(
                "/orders/" + orderId + "/pay",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class
        );

        // Then - 409 Conflict, order still OPEN
        assertEquals(HttpStatus.CONFLICT, payResponse.getStatusCode());

        ResponseEntity<String> orderResponse = restTemplate.getForEntity("/orders/" + orderId, String.class);
        assertEquals("OPEN", JsonPath.read(orderResponse.getBody(), "$.status"));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 3: Cancelling an order - all items also cancelled
    // -------------------------------------------------------------------------

    @Test
    void shouldCancelOrderAndAllItsItems() {
        // Given
        Long customerId = createCustomer("Piotr", "Wiśniewski");
        Long dishId = createDish("Sałatka", 1500);
        Long orderId = createOrder(customerId, dishId, 2);

        // When - cancel the order
        restTemplate.delete("/orders/" + orderId);

        // Then - zamówienie ma status CANCELLED
        ResponseEntity<String> orderResponse = restTemplate.getForEntity("/orders/" + orderId, String.class);
        assertEquals("CANCELLED", JsonPath.read(orderResponse.getBody(), "$.status"));

        // Then - order items are also marked as cancelled
        ResponseEntity<String> itemsResponse = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Boolean itemCancelled = JsonPath.read(itemsResponse.getBody(), "$[0].isCancelled");
        assertTrue(itemCancelled);
    }

    // -------------------------------------------------------------------------
    // SCENARIO 4: Cannot cancel a paid order
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn409WhenCancellingPaidOrder() {
        // Given - paid order
        Long customerId = createCustomer("Maria", "Kowalska");
        Long dishId = createDish("Zupa", 900);
        Long orderId = createOrder(customerId, dishId, 1);

        // change item status to DELIVERED
        ResponseEntity<String> itemsResponse = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Long itemId = ((Number) JsonPath.read(itemsResponse.getBody(), "$[0].id")).longValue();
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                Map.of("status", "DELIVERED"), String.class
        );

        // pay
        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        // When - attempt to cancel a paid order
        ResponseEntity<String> cancelResponse = restTemplate.exchange(
                "/orders/" + orderId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, cancelResponse.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 5: Adding items to an existing order
    // -------------------------------------------------------------------------

    @Test
    void shouldAddItemToExistingOpenOrder() {
        // Given
        Long customerId = createCustomer("Tomasz", "Nowicki");
        Long pizzaId = createDish("Pizza", 3200);
        Long burgerId = createDish("Burger", 2500);
        Long orderId = createOrder(customerId, pizzaId, 1); // order with 1 pizza

        // When - add a burger to the same order
        Map<String, Object> newItem = Map.of("dishId", burgerId, "quantity", 1, "seatNumber", 2);
        ResponseEntity<String> addResponse = restTemplate.postForEntity(
                "/orders/" + orderId + "/items", newItem, String.class
        );

        // Then - the order now has 2 items, total 5700
        assertEquals(HttpStatus.OK, addResponse.getStatusCode());
        Integer total = JsonPath.read(addResponse.getBody(), "$.totalPriceCents");
        assertEquals(5700, total); // 3200 + 2500
    }

    // -------------------------------------------------------------------------
    // SCENARIO 6: Cannot add items to a cancelled order
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn409WhenAddingItemToCancelledOrder() {
        // Given - cancelled order
        Long customerId = createCustomer("Karol", "Zając");
        Long dishId = createDish("Deser", 1200);
        Long orderId = createOrder(customerId, dishId, 1);
        restTemplate.delete("/orders/" + orderId);

        // When - attempt to add an item
        Map<String, Object> newItem = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/orders/" + orderId + "/items", newItem, String.class
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 7: The price of a dish in an order does not change after updating the dish
    // (price snapshot - key business scenario)
    // -------------------------------------------------------------------------

    @Test
    void orderItemShouldPreservePriceEvenAfterDishPriceUpdate() {
        // Given - order placed at price 3200
        Long customerId = createCustomer("Ewa", "Dąbrowska");
        Long dishId = createDish("Pizza", 3200);
        Long orderId = createOrder(customerId, dishId, 1);

        // When - update the dish price to 5000
        Map<String, Object> updateBody = Map.of("name", "Pizza", "description", "Opis", "priceInCents", 5000);
        restTemplate.put("/dishes/" + dishId, updateBody);

        // Then - the order still shows the old price 3200 (snapshot)
        ResponseEntity<String> orderResponse = restTemplate.getForEntity("/orders/" + orderId, String.class);
        Long total = ((Number) JsonPath.read(orderResponse.getBody(), "$.totalPriceCents")).longValue();
        assertEquals(3200L, total); // NOT 5000 - price frozen at the time of order
    }
}