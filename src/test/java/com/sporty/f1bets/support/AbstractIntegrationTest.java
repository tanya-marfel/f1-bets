package com.sporty.f1bets.support;

import com.sporty.f1bets.betting.application.UserRepository;
import com.sporty.f1bets.betting.domain.User;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.testing.Medium;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Base for full-stack tests: real HTTP against a random port, real Postgres via
 * Testcontainers, and the {@link FakeEventProvider} in place of openf1.org.
 * Tests use fresh users and distinct event ids to stay isolated within the
 * shared (cached) context. Responses are parsed as List/Map to stay independent
 * of the Jackson binding. The RestClient never throws on error statuses.
 */
@Medium
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, FakeEventProviderConfig.class})
public abstract class AbstractIntegrationTest {

    /** A decoded JSON object, e.g. a response body or an RFC 7807 problem detail. */
    protected static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {};

    /** A decoded JSON array of objects, e.g. the events listing. */
    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_OBJECT_LIST =
            new ParameterizedTypeReference<>() {};

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected UserRepository users;

    protected RestClient client;

    @BeforeEach
    void initRestClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(_ -> true, (_, _) -> {
                    // no-op: never throw, let tests assert on the status
                })
                .build();
    }

    protected long newUserWith(String balance) {
        return users.save(new User(Money.of(balance))).getId();
    }

    protected List<Map<String, Object>> listEvents() {
        return client.get().uri("/api/v1/events?year=2023").retrieve().body(JSON_OBJECT_LIST);
    }

    protected String quoteFor(long eventId, int driverId) {
        for (Map<String, Object> event : listEvents()) {
            if (((Number) event.get("eventId")).longValue() == eventId) {
                for (Map<String, Object> driver : driversOf(event)) {
                    if (((Number) driver.get("driverNumber")).intValue() == driverId) {
                        return (String) driver.get("quoteId");
                    }
                }
            }
        }
        throw new IllegalStateException("No quote for event " + eventId + " driver " + driverId);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> driversOf(Map<String, Object> event) {
        return (List<Map<String, Object>>) event.get("drivers");
    }

    protected ResponseEntity<Map<String, Object>> placeBet(long userId, String quoteId, String amount) {
        return placeRawBet(Map.of("userId", userId, "quoteId", quoteId, "amountEur", new BigDecimal(amount)));
    }

    protected ResponseEntity<Map<String, Object>> placeRawBet(Map<String, Object> body) {
        return client.post()
                .uri("/api/v1/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(JSON_OBJECT);
    }

    protected ResponseEntity<Map<String, Object>> settle(long eventId, int winningDriverId) {
        return client.post()
                .uri("/api/v1/outcomes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("eventId", eventId, "winningDriverId", winningDriverId))
                .retrieve()
                .toEntity(JSON_OBJECT);
    }
}
