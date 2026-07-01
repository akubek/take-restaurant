package pl.polsl.take.restaurant.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end reservation tests for table availability, conflicts, and
 * cancellation scenarios.
 */
class ReservationIntegrationTest extends BaseIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Long createCustomer(String firstName) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", "Testowy",
                "email", firstName.toLowerCase() + "@test.com");
        ResponseEntity<String> response = restTemplate.postForEntity("/customers", body, String.class);
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    private Long createDish() {
        Map<String, Object> body = Map.of(
                "name", "Pizza Rezerwacyjna",
                "description", "",
                "priceInCents", 2000,
                "calories", 500,
                "spiciness", "MILD");
        ResponseEntity<String> response = restTemplate.postForEntity("/dishes", body, String.class);
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    @Test
    void shouldCreateReservationForAvailableTable() {
        // Given: customer, dish, and free table time slot
        Long customerId = createCustomer("Jan");
        Long dishId = createDish();
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);
        Map<String, Object> body = Map.of(
                "customerId", customerId,
                "tableNumber", 5,
                "orderDateTime", tomorrow.format(FORMATTER),
                "items", List.of(item));

        // When: reservation is created
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);

        // Then: reservation is accepted and remains OPEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OPEN", JsonPath.read(response.getBody(), "$.status"));
        assertEquals(5, (Integer) JsonPath.read(response.getBody(), "$.tableNumber"));
    }

    @Test
    void shouldReturn409WhenTableAlreadyReservedAtSameTime() {
        // Given: existing reservation for the same table and overlapping time
        Long customer1 = createCustomer("Anna");
        Long customer2 = createCustomer("Piotr");
        Long dishId = createDish();

        LocalDateTime reservationTime = LocalDateTime.now()
                .plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);

        Map<String, Object> firstReservation = Map.of(
                "customerId", customer1,
                "tableNumber", 5,
                "orderDateTime", reservationTime.format(FORMATTER),
                "items", List.of(item));
        ResponseEntity<String> first = restTemplate.postForEntity("/orders", firstReservation, String.class);
        assertEquals(HttpStatus.OK, first.getStatusCode());

        Map<String, Object> secondReservation = Map.of(
                "customerId", customer2,
                "tableNumber", 5,
                "orderDateTime", reservationTime.plusMinutes(30).format(FORMATTER),
                "items", List.of(item));

        // When: second overlapping reservation is requested
        ResponseEntity<String> second = restTemplate.postForEntity("/orders", secondReservation, String.class);

        // Then: conflict is returned
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    }

    @Test
    void shouldAllowReservationsForDifferentTablesAtSameTime() {
        // Given: same reservation time but two different tables
        Long customer1 = createCustomer("Maria");
        Long customer2 = createCustomer("Tomasz");
        Long dishId = createDish();

        LocalDateTime sameTime = LocalDateTime.now()
                .plusDays(2).withHour(19).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);

        Map<String, Object> table3 = Map.of(
                "customerId", customer1,
                "tableNumber", 3,
                "orderDateTime", sameTime.format(FORMATTER),
                "items", List.of(item));
        ResponseEntity<String> res1 = restTemplate.postForEntity("/orders", table3, String.class);

        Map<String, Object> table7 = Map.of(
                "customerId", customer2,
                "tableNumber", 7,
                "orderDateTime", sameTime.format(FORMATTER),
                "items", List.of(item));
        ResponseEntity<String> res2 = restTemplate.postForEntity("/orders", table7, String.class);

        // Then: both reservations are accepted
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        assertEquals(HttpStatus.OK, res2.getStatusCode());
    }

    @Test
    void shouldAllowNewReservationAfterCancellation() {
        // Given: first reservation cancelled for table/time slot
        Long customer1 = createCustomer("Ewa");
        Long customer2 = createCustomer("Rafał");
        Long dishId = createDish();

        LocalDateTime time = LocalDateTime.now()
                .plusDays(3).withHour(20).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);
        Map<String, Object> reservationBody = Map.of(
                "customerId", customer1,
                "tableNumber", 4,
                "orderDateTime", time.format(FORMATTER),
                "items", List.of(item));

        ResponseEntity<String> firstRes = restTemplate.postForEntity("/orders", reservationBody, String.class);
        Long firstOrderId = ((Number) JsonPath.read(firstRes.getBody(), "$.id")).longValue();

        restTemplate.delete("/orders/" + firstOrderId);

        Map<String, Object> newReservation = Map.of(
                "customerId", customer2,
                "tableNumber", 4,
                "orderDateTime", time.format(FORMATTER),
                "items", List.of(item));

        // When: new reservation is created for the same slot
        ResponseEntity<String> secondRes = restTemplate.postForEntity("/orders", newReservation, String.class);

        // Then: slot is available again
        assertEquals(HttpStatus.OK, secondRes.getStatusCode());
    }

    @Test
    void shouldReturn409WhenCreatingReservationWithoutCustomer() {
        // Given: reservation payload without customer assignment
        Long dishId = createDish();
        LocalDateTime future = LocalDateTime.now().plusDays(1).format(FORMATTER) != null
                ? LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0)
                : LocalDateTime.now().plusDays(1);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);

        Map<String, Object> body = Map.of(
                "tableNumber", 2,
                "orderDateTime", future.format(FORMATTER),
                "items", List.of(item));

        // When: reservation is requested
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);

        // Then: conflict is returned
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}