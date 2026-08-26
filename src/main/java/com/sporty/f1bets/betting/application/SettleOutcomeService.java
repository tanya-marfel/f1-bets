package com.sporty.f1bets.betting.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.sporty.f1bets.betting.domain.Bet;
import com.sporty.f1bets.betting.domain.BetStatus;
import com.sporty.f1bets.betting.domain.EventOutcome;
import com.sporty.f1bets.shared.error.EventAlreadySettledException;
import com.sporty.f1bets.shared.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles a finished event: records the outcome, marks pending bets won/lost,
 * and credits winners. Idempotent via the event_outcomes primary key and
 * serialized against placement via the per-event advisory lock.
 */
@Service
@RequiredArgsConstructor
public class SettleOutcomeService {

    private final EventOutcomeRepository outcomes;
    private final BetRepository bets;
    private final WalletService wallet;
    private final EventLockRepository locks;
    private final Clock clock;

    @Transactional
    public SettlementResult settle(long eventId, int winningDriverId) {
        locks.lockEvent(eventId);

        // Under the per-event advisory lock, an existence check is race-free and
        // gives a clean 409. (EventOutcome has an assigned id, so save() would
        // merge rather than fail on the duplicate primary key.)
        if (outcomes.existsById(eventId)) {
            throw new EventAlreadySettledException(eventId);
        }
        outcomes.save(new EventOutcome(eventId, winningDriverId, Instant.now(clock)));

        List<Bet> pending = bets.findByEventIdAndStatus(eventId, BetStatus.PENDING);
        int won = 0;
        int lost = 0;
        Money totalPaidOut = Money.zero();

        for (Bet bet : pending) {
            if (bet.getDriverId() == winningDriverId) {
                bet.markWon();
                Money payout = bet.calculatePayout();
                wallet.credit(bet.getUserId(), payout);
                totalPaidOut = totalPaidOut.add(payout);
                won++;
            } else {
                bet.markLost();
                lost++;
            }
            bets.save(bet);
        }

        return new SettlementResult(eventId, winningDriverId, pending.size(), won, lost, totalPaidOut);
    }
}
