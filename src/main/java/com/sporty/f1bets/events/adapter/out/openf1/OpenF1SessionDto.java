package com.sporty.f1bets.events.adapter.out.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the openf1.org /sessions response we consume.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1SessionDto(
        @JsonProperty("session_key") long sessionKey,
        @JsonProperty("session_name") String sessionName,
        @JsonProperty("session_type") String sessionType,
        @JsonProperty("year") Integer year,
        @JsonProperty("country_name") String countryName) {}
