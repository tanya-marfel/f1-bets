package com.sporty.f1bets.support;

import com.sporty.f1bets.events.application.EventFilter;
import com.sporty.f1bets.events.application.EventProviderPort;
import com.sporty.f1bets.events.domain.Driver;
import com.sporty.f1bets.events.domain.Event;
import java.util.List;

/**
 * Deterministic in-memory {@link EventProviderPort} used by end-to-end and
 * concurrency tests, so they never touch openf1.org. Exposes a fixed set of
 * events (ids 100..500), each with two drivers (44 and 1).
 */
public class FakeEventProvider implements EventProviderPort {

    public static final int WINNING_DRIVER = 44;
    public static final int LOSING_DRIVER = 1;

    private final List<Event> events;

    public FakeEventProvider() {
        List<Driver> drivers =
                List.of(new Driver(WINNING_DRIVER, "Max Verstappen"), new Driver(LOSING_DRIVER, "Lewis Hamilton"));
        this.events = List.of(
                new Event(100L, "Race", 2023, "Italy", "Monza", drivers),
                new Event(200L, "Race", 2023, "Belgium", "Spa", drivers),
                new Event(300L, "Race", 2023, "Netherlands", "Zandvoort", drivers),
                new Event(400L, "Race", 2023, "Britain", "Silverstone", drivers),
                new Event(500L, "Race", 2023, "Hungary", "Hungaroring", drivers));
    }

    @Override
    public List<Event> listEvents(EventFilter filter) {
        return events;
    }
}
