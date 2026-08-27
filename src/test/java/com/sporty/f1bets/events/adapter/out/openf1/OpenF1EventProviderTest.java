package com.sporty.f1bets.events.adapter.out.openf1;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sporty.f1bets.events.application.EventFilter;
import com.sporty.f1bets.events.domain.Event;
import com.sporty.f1bets.shared.error.ProviderUnavailableException;
import com.sporty.f1bets.testing.Medium;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@Medium
class OpenF1EventProviderTest {

    private WireMockServer wireMock;
    private OpenF1EventProvider provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        provider = new OpenF1EventProvider(client);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void mapsSessionsAndDedupedDrivers() {
        wireMock.stubFor(get(urlPathEqualTo("/sessions"))
                .withQueryParam("year", equalTo("2023"))
                .willReturn(okJson("""
                        [{"session_key":100,"session_name":"Monza","session_type":"Race","year":2023,"country_name":"Italy"}]
                        """)));
        wireMock.stubFor(get(urlPathEqualTo("/drivers"))
                .withQueryParam("session_key", equalTo("100"))
                .willReturn(okJson("""
                        [
                          {"driver_number":44,"full_name":"Lewis Hamilton"},
                          {"driver_number":44,"full_name":"Lewis Hamilton"},
                          {"driver_number":1,"full_name":"Max Verstappen"},
                          {"driver_number":null,"full_name":"No Number"}
                        ]
                        """)));

        List<Event> events = provider.listEvents(new EventFilter(null, 2023, null));

        assertThat(events).hasSize(1);
        Event event = events.get(0);
        assertThat(event.eventId()).isEqualTo(100L);
        assertThat(event.country()).isEqualTo("Italy");
        assertThat(event.drivers()).extracting("driverNumber").containsExactly(44, 1);
    }

    @Test
    void upstreamServerErrorBecomesProviderUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo("/sessions"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(503)));
        EventFilter filter = new EventFilter(null, null, null);

        assertThatThrownBy(() -> provider.listEvents(filter)).isInstanceOf(ProviderUnavailableException.class);
    }
}
