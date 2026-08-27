package com.sporty.f1bets.shared.odds;

/**
 * Produces betting odds. The only allowed values are 2, 3 or 4.
 */
public interface OddsGenerator {

    int nextOdds();
}
