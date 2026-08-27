package com.sporty.f1bets.betting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.sporty.f1bets.betting.domain.BetStatus;
import com.sporty.f1bets.betting.domain.User;
import com.sporty.f1bets.shared.error.EventAlreadySettledException;
import com.sporty.f1bets.shared.error.InsufficientFundsException;
import com.sporty.f1bets.shared.error.InvalidBetAmountException;
import com.sporty.f1bets.shared.error.QuoteAlreadyUsedException;
import com.sporty.f1bets.shared.error.QuoteExpiredException;
import com.sporty.f1bets.shared.error.QuoteNotFoundException;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.shared.quote.OddsQuote;
import com.sporty.f1bets.shared.quote.OddsQuoteRepository;
import com.sporty.f1bets.testing.Small;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

@Small
class PlaceBetServiceTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");
    private static final long USER_ID = 1L;
    private static final long EVENT_ID = 100L;
    private static final int DRIVER_ID = 44;
    private static final UUID QUOTE_ID = UUID.randomUUID();

    private final OddsQuoteRepository quotes = mock(OddsQuoteRepository.class);
    private final EventOutcomeRepository outcomes = mock(EventOutcomeRepository.class);
    private final BetRepository bets = mock(BetRepository.class);
    private final WalletService wallet = mock(WalletService.class);
    private final EventLockRepository locks = mock(EventLockRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final PlaceBetService service =
            new PlaceBetService(quotes, outcomes, bets, wallet, locks, clock);

    private OddsQuote validQuote() {
        return new OddsQuote(QUOTE_ID, EVENT_ID, DRIVER_ID, 3, NOW.minusSeconds(10), NOW.plusSeconds(300));
    }

    @Test
    void placesBetAndReturnsNewBalance() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(validQuote()));
        when(outcomes.existsById(EVENT_ID)).thenReturn(false);
        when(wallet.debit(eq(USER_ID), any(Money.class))).thenReturn(new User(Money.of("75.00")));

        PlacedBet placed = service.placeBet(USER_ID, QUOTE_ID, Money.of("25.00"));

        assertThat(placed.status()).isEqualTo(BetStatus.PENDING);
        assertThat(placed.newBalance()).isEqualTo(Money.of("75.00"));
        verify(locks).lockEvent(EVENT_ID);
        verify(bets).saveAndFlush(any());
    }

    @Test
    void rejectsNonPositiveAmount() {
        Money amount = Money.zero();

        assertThatThrownBy(() -> service.placeBet(USER_ID, QUOTE_ID, amount))
                .isInstanceOf(InvalidBetAmountException.class);
        verify(quotes, never()).findById(any());
    }

    @Test
    void unknownQuoteThrows() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.empty());
        Money amount = Money.of("10.00");

        assertThatThrownBy(() -> service.placeBet(USER_ID, QUOTE_ID, amount))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void expiredQuoteThrows() {
        OddsQuote expired = new OddsQuote(QUOTE_ID, EVENT_ID, DRIVER_ID, 3, NOW.minusSeconds(600), NOW.minusSeconds(1));
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(expired));
        Money amount = Money.of("10.00");

        assertThatThrownBy(() -> service.placeBet(USER_ID, QUOTE_ID, amount))
                .isInstanceOf(QuoteExpiredException.class);
    }

    @Test
    void settledEventThrows() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(validQuote()));
        when(outcomes.existsById(EVENT_ID)).thenReturn(true);
        Money amount = Money.of("10.00");

        assertThatThrownBy(() -> service.placeBet(USER_ID, QUOTE_ID, amount))
                .isInstanceOf(EventAlreadySettledException.class);
        verify(wallet, never()).debit(anyLong(), any());
    }

    @Test
    void insufficientFundsPropagates() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(validQuote()));
        when(outcomes.existsById(EVENT_ID)).thenReturn(false);
        when(wallet.debit(eq(USER_ID), any(Money.class))).thenThrow(new InsufficientFundsException(USER_ID));
        Money amount = Money.of("1000.00");

        assertThatThrownBy(() -> service.placeBet(USER_ID, QUOTE_ID, amount))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void quoteAlreadyUsedThrowsOnUniqueViolation() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(validQuote()));
        when(outcomes.existsById(EVENT_ID)).thenReturn(false);
        when(wallet.debit(eq(USER_ID), any(Money.class))).thenReturn(new User(Money.of("75.00")));
        when(bets.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup quote_id"));
        Money amount = Money.of("25.00");

        assertThatThrownBy(() -> service.placeBet(USER_ID, QUOTE_ID, amount))
                .isInstanceOf(QuoteAlreadyUsedException.class);
    }
}




