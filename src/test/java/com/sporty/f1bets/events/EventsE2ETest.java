package com.sporty.f1bets.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.sporty.f1bets.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventsE2ETest extends AbstractIntegrationTest {

    @Test
    void filterlessCallIsRejectedWithBadRequest() {
        ResponseEntity<Map<String, Object>> response =
                client.get().uri("/api/v1/events").retrieve().toEntity(JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void yearFilterReturnsEvents() {
        List<Map<String, Object>> events = listEvents();

        assertThat(events).isNotEmpty();
    }
}
