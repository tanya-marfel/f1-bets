package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.BetStatus;
import com.sporty.f1bets.shared.money.Money;

/**
 * Result of placing a bet: the new bet id, its status, and the user's updated
 * balance.
 */
public record PlacedBet(Long betId, BetStatus status, Money newBalance) {}
