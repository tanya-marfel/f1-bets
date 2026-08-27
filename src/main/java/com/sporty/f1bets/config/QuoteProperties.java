package com.sporty.f1bets.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Time-to-live applied to issued odds quotes.
 */
@ConfigurationProperties(prefix = "quote")
public record QuoteProperties(Duration ttl) {

    public QuoteProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(5);
        }
    }
}
