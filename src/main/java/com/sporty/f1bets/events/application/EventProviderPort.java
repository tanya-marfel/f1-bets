package com.sporty.f1bets.events.application;

import com.sporty.f1bets.events.domain.Event;
import java.util.List;

/**
 * Outbound port for fetching F1 events from an external provider. This is the
 * single seam that keeps the system decoupled from openf1.org: adding a new
 * provider means adding a new adapter, nothing else changes.
 */
public interface EventProviderPort {

    List<Event> listEvents(EventFilter filter);
}
