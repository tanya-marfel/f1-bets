package com.sporty.f1bets.shared.error;

public class EventAlreadySettledException extends RuntimeException {

    private final long eventId;

    public EventAlreadySettledException(long eventId) {
        super("Event already settled: " + eventId);
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
}

