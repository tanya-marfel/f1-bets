package com.sporty.f1bets.events.adapter.out.openf1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sporty.f1bets.events.application.EventFilter;
import com.sporty.f1bets.events.application.EventProviderPort;
import com.sporty.f1bets.events.domain.Driver;
import com.sporty.f1bets.events.domain.Event;
import com.sporty.f1bets.shared.error.ProviderUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * openf1.org implementation of {@link EventProviderPort}. Translates upstream
 * transport failures into {@link ProviderUnavailableException} (mapped to 502).
 */
@Component
@RequiredArgsConstructor
public class OpenF1EventProvider implements EventProviderPort {

    private final RestClient restClient;

    @Override
    public List<Event> listEvents(EventFilter filter) {
        try {
            OpenF1SessionDto[] sessions = restClient.get()
                    .uri(builder -> {
                        builder.path("/sessions");
                        if (filter.sessionType() != null) {
                            builder.queryParam("session_type", filter.sessionType());
                        }
                        if (filter.year() != null) {
                            builder.queryParam("year", filter.year());
                        }
                        if (filter.country() != null) {
                            builder.queryParam("country_name", filter.country());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(OpenF1SessionDto[].class);

            if (sessions == null) {
                return List.of();
            }
            List<Event> events = new ArrayList<>(sessions.length);
            for (OpenF1SessionDto session : sessions) {
                List<Driver> drivers = fetchDrivers(session.sessionKey());
                events.add(new Event(
                        session.sessionKey(),
                        session.sessionType(),
                        session.year(),
                        session.countryName(),
                        session.sessionName(),
                        drivers));
            }
            return events;
        } catch (ResourceAccessException ex) {
            throw new ProviderUnavailableException("openf1.org is unavailable", ex);
        } catch (RestClientException ex) {
            throw new ProviderUnavailableException("openf1.org returned an error", ex);
        }
    }

    private List<Driver> fetchDrivers(long sessionKey) {
        OpenF1DriverDto[] drivers = restClient.get()
                .uri(builder -> builder.path("/drivers").queryParam("session_key", sessionKey).build())
                .retrieve()
                .body(OpenF1DriverDto[].class);

        if (drivers == null) {
            return List.of();
        }
        // openf1 can return duplicate driver rows per session; dedupe by number.
        Map<Integer, Driver> byNumber = new LinkedHashMap<>();
        for (OpenF1DriverDto dto : drivers) {
            if (dto.driverNumber() == null || dto.fullName() == null) {
                continue;
            }
            byNumber.putIfAbsent(dto.driverNumber(), new Driver(dto.driverNumber(), dto.fullName()));
        }
        return new ArrayList<>(byNumber.values());
    }
}
