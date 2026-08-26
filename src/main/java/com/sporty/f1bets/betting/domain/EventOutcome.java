package com.sporty.f1bets.betting.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The recorded outcome of a finished event. {@code eventId} is the primary key,
 * which makes settlement idempotent: a second settle of the same event fails on
 * the duplicate key.
 */
@Entity
@Table(name = "event_outcomes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventOutcome {

    @Id
    @Column(name = "event_id")
    private long eventId;

    @Column(name = "winning_driver_id", nullable = false)
    private int winningDriverId;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    public EventOutcome(long eventId, int winningDriverId, Instant settledAt) {
        this.eventId = eventId;
        this.winningDriverId = winningDriverId;
        this.settledAt = settledAt;
    }
}
