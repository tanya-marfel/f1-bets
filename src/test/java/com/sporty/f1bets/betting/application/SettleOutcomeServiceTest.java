package com.sporty.f1bets.betting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sporty.f1bets.betting.domain.Bet;
import com.sporty.f1bets.betting.domain.BetStatus;
import com.sporty.f1bets.shared.error.EventAlreadySettledException;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.testing.Small;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@Small
class SettleOutcomeServiceTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");
    private static final long EVENT_ID = 100L;
    private static final int WINNING_DRIVER = 44;

    private final EventOutcomeRepository outcomes = mock(EventOutcomeRepository.class);
    private final BetRepository bets = mock(BetRepository.class);
    private final WalletService wallet = mock(WalletService.class);
    private final EventLockRepository locks = mock(EventLockRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SettleOutcomeService service = new SettleOutcomeService(outcomes, bets, wallet, locks, clock);

    private Bet bet(long userId, int driverId, String amount, int odds) {
        return new Bet(userId, EVENT_ID, driverId, Money.of(amount), odds, UUID.randomUUID(), NOW);
    }

    @Test
    void settlesMixedBetsAndCreditsWinners() {
        Bet winnerA = bet(1L, WINNING_DRIVER, "10.00", 2); // payout 20.00
        Bet winnerB = bet(2L, WINNING_DRIVER, "5.00", 4); // payout 20.00
        Bet loser = bet(3L, 1, "10.00", 3);
        when(outcomes.existsById(EVENT_ID)).thenReturn(false);
        when(bets.findByEventIdAndStatus(EVENT_ID, BetStatus.PENDING)).thenReturn(List.of(winnerA, winnerB, loser));

        SettlementResult result = service.settle(EVENT_ID, WINNING_DRIVER);

        assertThat(result.settledBets()).isEqualTo(3);
        assertThat(result.wonBets()).isEqualTo(2);
        assertThat(result.lostBets()).isEqualTo(1);
        assertThat(result.totalPaidOut()).isEqualTo(Money.of("40.00"));
        assertThat(winnerA.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(loser.getStatus()).isEqualTo(BetStatus.LOST);
        verify(locks).lockEvent(EVENT_ID);
        verify(wallet).credit(1L, Money.of("20.00"));
        verify(wallet).credit(2L, Money.of("20.00"));
        verify(wallet, never()).credit(eq(3L), any());
        verify(bets, times(3)).save(any());
    }

    @Test
    void duplicateSettlementThrows() {
        when(outcomes.existsById(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.settle(EVENT_ID, WINNING_DRIVER))
                .isInstanceOf(EventAlreadySettledException.class);
        verify(bets, never()).findByEventIdAndStatus(anyLong(), any());
    }
}
