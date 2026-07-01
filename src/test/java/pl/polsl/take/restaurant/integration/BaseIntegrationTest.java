package pl.polsl.take.restaurant.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.IngredientRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
/**
 * Shared integration-test bootstrap with HTTP client and database cleanup between tests.
 */
public abstract class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected CustomerRepository customerRepository;
    @Autowired
    protected DishRepository dishRepository;
    @Autowired
    protected IngredientRepository ingredientRepository;

    @BeforeEach
    void cleanDatabase() {

        orderRepository.deleteAll();
        dishRepository.deleteAll();
        ingredientRepository.deleteAll();
        customerRepository.deleteAll();
    }
}