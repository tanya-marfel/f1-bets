package com.sporty.f1bets.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.sporty.f1bets.betting.application.UserRepository;
import com.sporty.f1bets.betting.domain.User;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.testing.Medium;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected UserRepository users;

    private RestClient client;

    @BeforeEach
    void initRestClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {
                    // no-op: never throw, let tests assert on the status
                })
                .build();
    }

    protected long newUserWith(String balance) {
        return users.save(new User(Money.of(balance))).getId();
    }

    protected List<?> listEvents() {
        return client.get().uri("/api/v1/events?year=2023").retrieve().body(List.class);
    }

    @SuppressWarnings("unchecked")
    protected String quoteFor(long eventId, int driverId) {
        for (Object eventObj : listEvents()) {
            Map<String, Object> event = (Map<String, Object>) eventObj;
            if (((Number) event.get("eventId")).longValue() == eventId) {
                for (Object driverObj : (List<Object>) event.get("drivers")) {
                    Map<String, Object> driver = (Map<String, Object>) driverObj;
                    if (((Number) driver.get("driverNumber")).intValue() == driverId) {
                        return (String) driver.get("quoteId");
                    }
                }
            }
        }
        throw new IllegalStateException("No quote for event " + eventId + " driver " + driverId);
    }

    protected ResponseEntity<Map> placeBet(long userId, String quoteId, String amount) {
        return placeRawBet(Map.of("userId", userId, "quoteId", quoteId, "amountEur", new BigDecimal(amount)));
    }

    protected ResponseEntity<Map> placeRawBet(Map<String, Object> body) {
        return client.post().uri("/api/v1/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
    }

    protected ResponseEntity<Map> settle(long eventId, int winningDriverId) {
        return client.post().uri("/api/v1/outcomes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("eventId", eventId, "winningDriverId", winningDriverId))
                .retrieve()
                .toEntity(Map.class);
    }
}

