package com.sporty.f1bets.events.adapter.in.web;

import com.sporty.f1bets.events.application.EventFilter;
import com.sporty.f1bets.events.application.EventMarket;
import com.sporty.f1bets.events.application.ListEventsService;
import com.sporty.f1bets.generated.api.EventsApi;
import com.sporty.f1bets.generated.model.DriverMarketResponse;
import com.sporty.f1bets.generated.model.EventResponse;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the contract-first {@link EventsApi} generated from the OpenAPI
 * spec, mapping the domain market to the generated DTOs.
 */
@RestController
@RequiredArgsConstructor
public class EventsController implements EventsApi {

    private final ListEventsService listEventsService;

    @Override
    public ResponseEntity<List<EventResponse>> listEvents(Integer year, String sessionType, String country) {
        List<EventResponse> events = listEventsService.list(new EventFilter(sessionType, year, country)).stream()
                .map(EventsController::toResponse)
                .toList();
        return ResponseEntity.ok(events);
    }

    private static EventResponse toResponse(EventMarket eventMarket) {
        EventResponse response = new EventResponse()
                .eventId(eventMarket.event().eventId())
                .sessionType(eventMarket.event().sessionType())
                .year(eventMarket.event().year())
                .country(eventMarket.event().country())
                .sessionName(eventMarket.event().sessionName());
        eventMarket
                .market()
                .forEach(entry -> response.addDriversItem(new DriverMarketResponse()
                        .driverNumber(entry.driver().driverNumber())
                        .fullName(entry.driver().fullName())
                        .odds(entry.odds())
                        .quoteId(entry.quoteId())
                        .quoteExpiresAt(entry.quoteExpiresAt().atOffset(ZoneOffset.UTC))));
        return response;
    }
}
