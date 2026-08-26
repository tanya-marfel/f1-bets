package com.sporty.f1bets.events.application;

import java.util.List;

import com.sporty.f1bets.events.domain.DriverMarketEntry;
import com.sporty.f1bets.events.domain.Event;

/**
 * An event enriched with its driver market (odds + issued quotes).
 */
public record EventMarket(Event event, List<DriverMarketEntry> market) {
}

