package com.sporty.f1bets.events.domain;

import java.util.List;

/**
 * An F1 event (called a "session" by openf1.org) together with its drivers.
 * Pure domain: no persistence, no framework types.
 */
public record Event(long eventId, String sessionType, Integer year, String country, String sessionName,
                    List<Driver> drivers) {
}

