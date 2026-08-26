package com.sporty.f1bets.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.sporty.f1bets.config.QuoteProperties;
import com.sporty.f1bets.events.domain.Driver;
import com.sporty.f1bets.events.domain.DriverMarketEntry;
import com.sporty.f1bets.events.domain.Event;
import com.sporty.f1bets.shared.odds.OddsGenerator;
import com.sporty.f1bets.shared.quote.OddsQuote;
import com.sporty.f1bets.shared.quote.OddsQuoteRepository;
import com.sporty.f1bets.testing.Small;
import org.junit.jupiter.api.Test;

@Small
class ListEventsServiceTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private final EventProviderPort provider = mock(EventProviderPort.class);
    private final OddsQuoteRepository quotes = mock(OddsQuoteRepository.class);
    private final OddsGenerator oddsGenerator = mock(OddsGenerator.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ListEventsService service = new ListEventsService(
            provider, quotes, oddsGenerator, clock, new QuoteProperties(Duration.ofMinutes(5)));

    @Test
    void issuesAQuotePerDriverWithConfiguredExpiry() {
        Driver max = new Driver(44, "Max");
        Driver lewis = new Driver(1, "Lewis");
        Event event = new Event(100L, "Race", 2023, "Italy", "Monza", List.of(max, lewis));
        when(provider.listEvents(any())).thenReturn(List.of(event));
        when(oddsGenerator.nextOdds()).thenReturn(3, 4);

        List<EventMarket> result = service.list(new EventFilter(null, 2023, null));

        Instant expiry = NOW.plusSeconds(300);
        assertThat(result).singleElement().satisfies(eventMarket -> {
            assertThat(eventMarket.event()).isEqualTo(event);
            // Compare market entries against expected objects; quoteId is
            // server-generated (random), so it's ignored here and checked non-null below.
            assertThat(eventMarket.market())
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("quoteId")
                    .containsExactly(
                            new DriverMarketEntry(max, 3, null, expiry),
                            new DriverMarketEntry(lewis, 4, null, expiry));
            assertThat(eventMarket.market()).allSatisfy(entry -> assertThat(entry.quoteId()).isNotNull());
        });
        verify(quotes, times(2)).save(any(OddsQuote.class));
    }

    @Test
    void emptyProviderYieldsEmptyResult() {
        when(provider.listEvents(any())).thenReturn(List.of());

        assertThat(service.list(new EventFilter(null, null, null))).isEmpty();
    }
}




