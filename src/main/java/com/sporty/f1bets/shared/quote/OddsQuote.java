package com.sporty.f1bets.shared.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A server-issued, single-use, expiring odds quote. Issued when events are
 * listed; consumed when a bet is placed. Because the server owns the odds,
 * clients cannot influence the price they bet at.
 */
@Entity
@Table(name = "odds_quotes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OddsQuote {

    @Id
    @Column(name = "quote_id")
    private UUID quoteId;

    @Column(name = "event_id", nullable = false)
    private long eventId;

    @Column(name = "driver_id", nullable = false)
    private int driverId;

    @Column(nullable = false)
    private int odds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public OddsQuote(UUID quoteId, long eventId, int driverId, int odds, Instant createdAt, Instant expiresAt) {
        this.quoteId = quoteId;
        this.eventId = eventId;
        this.driverId = driverId;
        this.odds = odds;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Clock clock) {
        return !Instant.now(clock).isBefore(expiresAt);
    }
}
