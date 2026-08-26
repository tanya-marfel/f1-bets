package com.sporty.f1bets.betting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.testing.Small;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Small
class BetTest {

    private Bet newBet(int odds) {
        return new Bet(1L, 100L, 44, Money.of("25.00"), odds, UUID.randomUUID(), Instant.now());
    }

    @ParameterizedTest
    @CsvSource({
            "2, 50.00",
            "3, 75.00",
            "4, 100.00"
    })
    void payoutIsStakeTimesOdds(int odds, String expectedPayout) {
        assertThat(newBet(odds).calculatePayout()).isEqualTo(Money.of(expectedPayout));
    }

    @Test
    void startsPendingThenTransitions() {
        Bet bet = newBet(2);
        assertThat(bet.isPending()).isTrue();
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PENDING);

        bet.markWon();
        assertThat(bet.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(bet.isPending()).isFalse();

        Bet other = newBet(2);
        other.markLost();
        assertThat(other.getStatus()).isEqualTo(BetStatus.LOST);
    }
}


