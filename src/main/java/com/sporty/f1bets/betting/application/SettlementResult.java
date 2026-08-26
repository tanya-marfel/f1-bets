package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.shared.money.Money;

/**
 * Summary of a settlement run.
 */
public record SettlementResult(long eventId, int winningDriverId, int settledBets, int wonBets, int lostBets,
                               Money totalPaidOut) {
}
