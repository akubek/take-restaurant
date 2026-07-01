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
 * End-to-end order lifecycle tests from creation through status transitions and
 * payment.
 */
class OrderLifecycleIntegrationTest extends BaseIntegrationTest {

    private Long createCustomer(String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", firstName.toLowerCase() + "@test.com");
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
                "spiciness", "MILD");
        ResponseEntity<String> response = restTemplate.postForEntity("/dishes", body, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    private Long createOrder(Long customerId, Long dishId, int quantity) {
        Map<String, Object> item = Map.of("dishId", dishId, "quantity", quantity, "seatNumber", 1);
        Map<String, Object> body = Map.of(
                "customerId", customerId,
                "tableNumber", 5,
                "items", List.of(item));
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    @Test
    void fullOrderLifecycle_fromCreationToPayment() {
        // Given: existing customer and dish
        Long customerId = createCustomer("Jan", "Kowalski");
        Long dishId = createDish("Pizza Margherita", 3200);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 2, "seatNumber", 1);
        Map<String, Object> orderBody = Map.of(
                "customerId", customerId,
                "tableNumber", 5,
                "items", List.of(item));

        // When: order is created
        ResponseEntity<String> createResponse = restTemplate.postForEntity("/orders", orderBody, String.class);

        // Then: order is open with expected total
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        Long orderId = ((Number) JsonPath.read(createResponse.getBody(), "$.id")).longValue();
        assertEquals("OPEN", JsonPath.read(createResponse.getBody(), "$.status"));
        assertEquals(6400L, ((Number) JsonPath.read(createResponse.getBody(), "$.totalPriceCents")).longValue());

        ResponseEntity<String> itemsResponse = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        assertEquals(HttpStatus.OK, itemsResponse.getStatusCode());
        Long itemId = ((Number) JsonPath.read(itemsResponse.getBody(), "$[0].id")).longValue();

        Map<String, String> statusBody = Map.of("status", "DELIVERED");

        // When: item is delivered and order is paid
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                statusBody, String.class);

        ResponseEntity<String> payResponse = restTemplate.exchange(
                "/orders/" + orderId + "/pay",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class);

        // Then: order is closed as PAID with unchanged total
        assertEquals(HttpStatus.OK, payResponse.getStatusCode());
        assertEquals("PAID", JsonPath.read(payResponse.getBody(), "$.status"));
        assertEquals(6400L, ((Number) JsonPath.read(payResponse.getBody(), "$.totalPriceCents")).longValue());
    }

    @Test
    void shouldReturn409WhenPayingOrderWithPendingItems() {
        // Given: open order with item not delivered yet
        Long customerId = createCustomer("Anna", "Nowak");
        Long dishId = createDish("Burger", 2500);
        Long orderId = createOrder(customerId, dishId, 1);

        // When: payment is requested
        ResponseEntity<String> payResponse = restTemplate.exchange(
                "/orders/" + orderId + "/pay",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class);

        // Then: conflict is returned and order remains OPEN
        assertEquals(HttpStatus.CONFLICT, payResponse.getStatusCode());

        ResponseEntity<String> orderResponse = restTemplate.getForEntity("/orders/" + orderId, String.class);
        assertEquals("OPEN", JsonPath.read(orderResponse.getBody(), "$.status"));
    }

    @Test
    void shouldCancelOrderAndAllItsItems() {
        // Given: open order with multiple items
        Long customerId = createCustomer("Piotr", "Wiśniewski");
        Long dishId = createDish("Sałatka", 1500);
        Long orderId = createOrder(customerId, dishId, 2);

        // When: order is cancelled
        restTemplate.delete("/orders/" + orderId);

        // Then: order and items are marked as cancelled
        ResponseEntity<String> orderResponse = restTemplate.getForEntity("/orders/" + orderId, String.class);
        assertEquals("CANCELLED", JsonPath.read(orderResponse.getBody(), "$.status"));

        ResponseEntity<String> itemsResponse = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Boolean itemCancelled = JsonPath.read(itemsResponse.getBody(), "$[0].isCancelled");
        assertTrue(itemCancelled);
    }

    @Test
    void shouldReturn409WhenCancellingPaidOrder() {
        // Given: already paid order
        Long customerId = createCustomer("Maria", "Kowalska");
        Long dishId = createDish("Zupa", 900);
        Long orderId = createOrder(customerId, dishId, 1);

        ResponseEntity<String> itemsResponse = restTemplate.getForEntity("/orders/" + orderId + "/items", String.class);
        Long itemId = ((Number) JsonPath.read(itemsResponse.getBody(), "$[0].id")).longValue();
        restTemplate.patchForObject(
                "/orders/" + orderId + "/items/" + itemId + "/status",
                Map.of("status", "DELIVERED"), String.class);

        restTemplate.exchange("/orders/" + orderId + "/pay", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        // When: cancellation is requested
        ResponseEntity<String> cancelResponse = restTemplate.exchange(
                "/orders/" + orderId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);

        // Then: cancellation is rejected
        assertEquals(HttpStatus.CONFLICT, cancelResponse.getStatusCode());
    }

    @Test
    void shouldAddItemToExistingOpenOrder() {
        // Given: existing open order and another dish
        Long customerId = createCustomer("Tomasz", "Nowicki");
        Long pizzaId = createDish("Pizza", 3200);
        Long burgerId = createDish("Burger", 2500);
        Long orderId = createOrder(customerId, pizzaId, 1);

        Map<String, Object> newItem = Map.of("dishId", burgerId, "quantity", 1, "seatNumber", 2);

        // When: new item is added to order
        ResponseEntity<String> addResponse = restTemplate.postForEntity(
                "/orders/" + orderId + "/items", newItem, String.class);

        // Then: updated total includes both items
        assertEquals(HttpStatus.OK, addResponse.getStatusCode());
        Integer total = JsonPath.read(addResponse.getBody(), "$.totalPriceCents");
        assertEquals(5700, total);
    }

    @Test
    void shouldReturn409WhenAddingItemToCancelledOrder() {
        // Given: cancelled order
        Long customerId = createCustomer("Karol", "Zając");
        Long dishId = createDish("Deser", 1200);
        Long orderId = createOrder(customerId, dishId, 1);
        restTemplate.delete("/orders/" + orderId);

        Map<String, Object> newItem = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);

        // When: adding item to cancelled order
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/orders/" + orderId + "/items", newItem, String.class);

        // Then: conflict is returned
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void orderItemShouldPreservePriceEvenAfterDishPriceUpdate() {
        // Given: order created with initial dish price
        Long customerId = createCustomer("Ewa", "Dąbrowska");
        Long dishId = createDish("Pizza", 3200);
        Long orderId = createOrder(customerId, dishId, 1);

        // When: dish price is updated after order creation
        Map<String, Object> updateBody = Map.of("name", "Pizza", "description", "Opis", "priceInCents", 5000);
        restTemplate.put("/dishes/" + dishId, updateBody);

        // Then: order total still uses original item price snapshot
        ResponseEntity<String> orderResponse = restTemplate.getForEntity("/orders/" + orderId, String.class);
        Long total = ((Number) JsonPath.read(orderResponse.getBody(), "$.totalPriceCents")).longValue();
        assertEquals(3200L, total);
    }
}