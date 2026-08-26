package com.sporty.f1bets.events.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sporty.f1bets.config.QuoteProperties;
import com.sporty.f1bets.events.domain.Driver;
import com.sporty.f1bets.events.domain.DriverMarketEntry;
import com.sporty.f1bets.events.domain.Event;
import com.sporty.f1bets.shared.odds.OddsGenerator;
import com.sporty.f1bets.shared.quote.OddsQuote;
import com.sporty.f1bets.shared.quote.OddsQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists events from the provider and, for every driver, issues a single-use
 * expiring odds quote. Membership is therefore validated at issuance time:
 * a client can only bet by referencing a quote the server minted for a real
 * driver in a real event.
 */
@Service
@RequiredArgsConstructor
public class ListEventsService {

    private final EventProviderPort provider;
    private final OddsQuoteRepository quotes;
    private final OddsGenerator oddsGenerator;
    private final Clock clock;
    private final QuoteProperties quoteProperties;

    @Transactional
    public List<EventMarket> list(EventFilter filter) {
        var events = provider.listEvents(filter);
        var now = Instant.now(clock);
        var expiresAt = now.plus(quoteProperties.ttl());

        var result = new ArrayList<EventMarket>(events.size());
        for (var event : events) {
            result.add(new EventMarket(event, openDriverMarket(event, now, expiresAt)));
        }
        return result;
    }

    /**
     * Issues a single-use odds quote for each driver in the event and returns
     * the resulting driver market.
     */
    private List<DriverMarketEntry> openDriverMarket(Event event, Instant now, Instant expiresAt) {
        var market = new ArrayList<DriverMarketEntry>(event.drivers().size());
        for (Driver driver : event.drivers()) {
            int odds = oddsGenerator.nextOdds();
            UUID quoteId = UUID.randomUUID();
            quotes.save(new OddsQuote(quoteId, event.eventId(), driver.driverNumber(), odds, now, expiresAt));
            market.add(new DriverMarketEntry(driver, odds, quoteId, expiresAt));
        }
        return market;
    }
}
