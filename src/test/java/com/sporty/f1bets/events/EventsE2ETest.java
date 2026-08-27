package com.sporty.f1bets.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.sporty.f1bets.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventsE2ETest extends AbstractIntegrationTest {

    @Test
    void filterlessCallIsRejectedWithBadRequest() {
        ResponseEntity<Map> response = client.get()
                .uri("/api/v1/events")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void yearFilterReturnsEvents() {
        List<?> events = listEvents();

        assertThat(events).isNotEmpty();
    }
}
