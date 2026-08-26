package com.sporty.f1bets.shared.quote;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.sporty.f1bets.testing.Small;
import org.junit.jupiter.api.Test;

@Small
class OddsQuoteTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void notExpiredBeforeExpiry() {
        OddsQuote quote = new OddsQuote(UUID.randomUUID(), 1L, 44, 3, NOW, NOW.plusSeconds(300));
        assertThat(quote.isExpired(Clock.fixed(NOW.plusSeconds(299), ZoneOffset.UTC))).isFalse();
    }

    @Test
    void expiredAtAndAfterExpiry() {
        OddsQuote quote = new OddsQuote(UUID.randomUUID(), 1L, 44, 3, NOW, NOW.plusSeconds(300));
        assertThat(quote.isExpired(Clock.fixed(NOW.plusSeconds(300), ZoneOffset.UTC))).isTrue();
        assertThat(quote.isExpired(Clock.fixed(NOW.plusSeconds(301), ZoneOffset.UTC))).isTrue();
    }
}
