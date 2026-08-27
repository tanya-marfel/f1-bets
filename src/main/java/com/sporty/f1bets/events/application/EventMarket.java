package com.sporty.f1bets.events.application;

import com.sporty.f1bets.events.domain.DriverMarketEntry;
import com.sporty.f1bets.events.domain.Event;
import java.util.List;

/**
 * An event enriched with its driver market (odds + issued quotes).
 */
public record EventMarket(Event event, List<DriverMarketEntry> market) {}
