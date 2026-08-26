package com.sporty.f1bets.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the openf1.org provider. Kept behind this class so the
 * rest of the app never hard-codes the upstream URL or timeouts.
 */
@ConfigurationProperties(prefix = "openf1")
public record OpenF1Properties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public OpenF1Properties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openf1.org/v1";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(5);
        }
    }
}

