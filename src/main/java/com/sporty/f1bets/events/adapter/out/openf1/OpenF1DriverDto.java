package com.sporty.f1bets.events.adapter.out.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the openf1.org /drivers response we consume.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1DriverDto(
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("full_name") String fullName) {
}

