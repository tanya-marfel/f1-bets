package com.sporty.f1bets.betting.domain;

import com.sporty.f1bets.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single bet on a driver to win an event. Odds are snapshotted at placement
 * so settlement is independent of the quote lifecycle. The unique quote_id
 * column enforces single-use of a quote.
 */
@Entity
@Table(name = "bets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "event_id", nullable = false)
    private long eventId;

    @Column(name = "driver_id", nullable = false)
    private int driverId;

    @Column(name = "amount_eur", nullable = false)
    private Money amount;

    @Column(name = "odds_at_placement", nullable = false)
    private int oddsAtPlacement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetStatus status;

    @Column(name = "quote_id", nullable = false, unique = true)
    private UUID quoteId;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    public Bet(
            long userId,
            long eventId,
            int driverId,
            Money amount,
            int oddsAtPlacement,
            UUID quoteId,
            Instant placedAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.driverId = driverId;
        this.amount = amount;
        this.oddsAtPlacement = oddsAtPlacement;
        this.quoteId = quoteId;
        this.placedAt = placedAt;
        this.status = BetStatus.PENDING;
    }

    /**
     * Total return on a winning bet: stake x odds. The stake was already
     * debited at placement, so the full amount is credited on a win.
     */
    public Money calculatePayout() {
        return amount.multiply(oddsAtPlacement);
    }

    public void markWon() {
        this.status = BetStatus.WON;
    }

    public void markLost() {
        this.status = BetStatus.LOST;
    }

    public boolean isPending() {
        return status == BetStatus.PENDING;
    }
}
