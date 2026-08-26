package com.sporty.f1bets.shared.odds;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Returns a random integer from {2, 3, 4}. A {@link RandomGenerator} can be
 * injected to make tests deterministic; otherwise ThreadLocalRandom is used.
 */
public class RandomOddsGenerator implements OddsGenerator {

    private static final int[] ALLOWED = {2, 3, 4};

    private final RandomGenerator random;

    public RandomOddsGenerator() {
        this.random = null;
    }

    public RandomOddsGenerator(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public int nextOdds() {
        RandomGenerator generator = random != null ? random : ThreadLocalRandom.current();
        return ALLOWED[generator.nextInt(ALLOWED.length)];
    }
}

