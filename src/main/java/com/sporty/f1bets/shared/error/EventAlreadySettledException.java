package com.sporty.f1bets.shared.error;

import lombok.Getter;

@Getter
public class EventAlreadySettledException extends RuntimeException {

    private final long eventId;

    public EventAlreadySettledException(long eventId) {
        super("Event already settled: " + eventId);
        this.eventId = eventId;
    }
}
