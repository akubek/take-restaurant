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

// Test use case scenarios:
// "Creating an order as a reservation - checking table availability at a given time"
class ReservationIntegrationTest extends BaseIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Long createCustomer(String firstName) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", "Testowy",
                "email", firstName.toLowerCase() + "@test.com"
        );
        ResponseEntity<String> response = restTemplate.postForEntity("/customers", body, String.class);
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    private Long createDish() {
        Map<String, Object> body = Map.of(
                "name", "Pizza Rezerwacyjna",
                "description", "",
                "priceInCents", 2000,
                "calories", 500,
                "spiciness", "MILD"
        );
        ResponseEntity<String> response = restTemplate.postForEntity("/dishes", body, String.class);
        return ((Number) JsonPath.read(response.getBody(), "$.id")).longValue();
    }

    // -------------------------------------------------------------------------
    // SCENARIO 1: Successful reservation for an available table
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateReservationForAvailableTable() {
        // Given
        Long customerId = createCustomer("Jan");
        Long dishId = createDish();
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);
        Map<String, Object> body = Map.of(
                "customerId", customerId,
                "tableNumber", 5,
                "orderDateTime", tomorrow.format(FORMATTER),
                "items", List.of(item)
        );

        // When
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);

        // Then - reservation accepted
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OPEN", JsonPath.read(response.getBody(), "$.status"));
        assertEquals(5, (Integer) JsonPath.read(response.getBody(), "$.tableNumber"));
    }

    // -------------------------------------------------------------------------
    // SCENARIO 2: Reservation conflict - same table, overlapping time
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn409WhenTableAlreadyReservedAtSameTime() {
        // Given - first reservation for table 5 tomorrow at 18:00
        Long customer1 = createCustomer("Anna");
        Long customer2 = createCustomer("Piotr");
        Long dishId = createDish();

        LocalDateTime reservationTime = LocalDateTime.now()
                .plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);

        // First reservation
        Map<String, Object> firstReservation = Map.of(
                "customerId", customer1,
                "tableNumber", 5,
                "orderDateTime", reservationTime.format(FORMATTER),
                "items", List.of(item)
        );
        ResponseEntity<String> first = restTemplate.postForEntity("/orders", firstReservation, String.class);
        assertEquals(HttpStatus.OK, first.getStatusCode());

        // When - second reservation for the same table 30 minutes later (within 2-hour window)
        Map<String, Object> secondReservation = Map.of(
                "customerId", customer2,
                "tableNumber", 5,
                "orderDateTime", reservationTime.plusMinutes(30).format(FORMATTER),
                "items", List.of(item)
        );
        ResponseEntity<String> second = restTemplate.postForEntity("/orders", secondReservation, String.class);

        // Then - 409 Conflict - table occupied
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 3: Different tables at the same time - should work
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowReservationsForDifferentTablesAtSameTime() {
        // Given
        Long customer1 = createCustomer("Maria");
        Long customer2 = createCustomer("Tomasz");
        Long dishId = createDish();

        LocalDateTime sameTime = LocalDateTime.now()
                .plusDays(2).withHour(19).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);

        // When - reservation for table 3
        Map<String, Object> table3 = Map.of(
                "customerId", customer1,
                "tableNumber", 3,
                "orderDateTime", sameTime.format(FORMATTER),
                "items", List.of(item)
        );
        ResponseEntity<String> res1 = restTemplate.postForEntity("/orders", table3, String.class);

        // reservation for table 7 at the same time
        Map<String, Object> table7 = Map.of(
                "customerId", customer2,
                "tableNumber", 7,
                "orderDateTime", sameTime.format(FORMATTER),
                "items", List.of(item)
        );
        ResponseEntity<String> res2 = restTemplate.postForEntity("/orders", table7, String.class);

        // Then - both reservations accepted (different tables)
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        assertEquals(HttpStatus.OK, res2.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 4: Cancelled reservation frees up the table
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowNewReservationAfterCancellation() {
        // Given - reservation and then its cancellation
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
                "items", List.of(item)
        );

        // First reservation
        ResponseEntity<String> firstRes = restTemplate.postForEntity("/orders", reservationBody, String.class);
        Long firstOrderId = ((Number) JsonPath.read(firstRes.getBody(), "$.id")).longValue();

        // Cancellation
        restTemplate.delete("/orders/" + firstOrderId);

        // When - new reservation for the same table and time
        Map<String, Object> newReservation = Map.of(
                "customerId", customer2,
                "tableNumber", 4,
                "orderDateTime", time.format(FORMATTER),
                "items", List.of(item)
        );
        ResponseEntity<String> secondRes = restTemplate.postForEntity("/orders", newReservation, String.class);

        // Then - table available after cancelling the previous reservation
        assertEquals(HttpStatus.OK, secondRes.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // SCENARIO 5: Reservation requires a customer (cannot be anonymous)
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn409WhenCreatingReservationWithoutCustomer() {
        // Given
        Long dishId = createDish();
        LocalDateTime future = LocalDateTime.now().plusDays(1).format(FORMATTER) != null
                ? LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0)
                : LocalDateTime.now().plusDays(1);

        Map<String, Object> item = Map.of("dishId", dishId, "quantity", 1, "seatNumber", 1);
        // No customerId - anonymous order as reservation
        Map<String, Object> body = Map.of(
                "tableNumber", 2,
                "orderDateTime", future.format(FORMATTER),
                "items", List.of(item)
        );

        // When
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);

        // Then - reservation without a customer is not allowed
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}