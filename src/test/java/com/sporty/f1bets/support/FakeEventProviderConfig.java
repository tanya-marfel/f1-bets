package com.sporty.f1bets.support;

import com.sporty.f1bets.events.application.EventProviderPort;
import com.sporty.f1bets.shared.odds.OddsGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Overrides the real openf1.org adapter with {@link FakeEventProvider} for
 * full-stack tests, and pins odds to a fixed value so payout maths are
 * deterministic.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeEventProviderConfig {

    /** Fixed odds used by full-stack tests (stake x 4). */
    public static final int FIXED_ODDS = 4;

    @Bean
    @Primary
    EventProviderPort fakeEventProvider() {
        return new FakeEventProvider();
    }

    @Bean
    @Primary
    OddsGenerator fixedOddsGenerator() {
        return () -> FIXED_ODDS;
    }
}
