package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.Bet;
import com.sporty.f1bets.betting.domain.User;
import com.sporty.f1bets.shared.error.EventAlreadySettledException;
import com.sporty.f1bets.shared.error.InvalidBetAmountException;
import com.sporty.f1bets.shared.error.QuoteAlreadyUsedException;
import com.sporty.f1bets.shared.error.QuoteExpiredException;
import com.sporty.f1bets.shared.error.QuoteNotFoundException;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.shared.quote.OddsQuote;
import com.sporty.f1bets.shared.quote.OddsQuoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places a single bet by consuming a server-issued quote. All work happens in
 * one transaction guarded by a per-event advisory lock.
 */
@Service
@RequiredArgsConstructor
public class PlaceBetService {

    private final OddsQuoteRepository quotes;
    private final EventOutcomeRepository outcomes;
    private final BetRepository bets;
    private final WalletService wallet;
    private final EventLockRepository locks;
    private final Clock clock;

    @Transactional
    public PlacedBet placeBet(long userId, UUID quoteId, Money amount) {
        if (!amount.isPositive()) {
            throw new InvalidBetAmountException("Bet amount must be greater than zero");
        }

        OddsQuote quote = quotes.findById(quoteId).orElseThrow(() -> new QuoteNotFoundException(quoteId));
        if (quote.isExpired(clock)) {
            throw new QuoteExpiredException(quoteId);
        }

        // Serialize placement and settlement for this event.
        locks.lockEvent(quote.getEventId());

        if (outcomes.existsById(quote.getEventId())) {
            throw new EventAlreadySettledException(quote.getEventId());
        }

        User user = wallet.debit(userId, amount);

        Bet bet = new Bet(
                userId, quote.getEventId(), quote.getDriverId(), amount, quote.getOdds(), quoteId, Instant.now(clock));
        try {
            bets.saveAndFlush(bet);
        } catch (DataIntegrityViolationException _) {
            // Unique violation on quote_id: the quote was already consumed.
            throw new QuoteAlreadyUsedException(quoteId);
        }

        return new PlacedBet(bet.getId(), bet.getStatus(), user.getBalance());
    }
}
