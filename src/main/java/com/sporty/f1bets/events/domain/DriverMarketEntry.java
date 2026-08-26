package com.sporty.f1bets.events.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A driver in an event's market: the driver, the offered odds, and the
 * server-issued quote a client must reference to bet at these odds.
 */
public record DriverMarketEntry(Driver driver, int odds, UUID quoteId, Instant quoteExpiresAt) {
}

